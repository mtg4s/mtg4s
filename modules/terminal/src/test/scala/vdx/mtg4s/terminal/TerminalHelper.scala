package vdx.mtg4s.terminal

import scala.annotation.tailrec

import cats.instances.int._
import cats.syntax.eq._

object TerminalHelper {

  sealed trait ControlSeqenceChar
  case object Escape extends ControlSeqenceChar
  case object LeftSquareBracket extends ControlSeqenceChar
  case object SemiColon extends ControlSeqenceChar
  case class Number(digits: List[Int]) extends ControlSeqenceChar
  case class CapitalLetter(c: Char) extends ControlSeqenceChar
  case class Unknown(char: Int) extends ControlSeqenceChar

  def byteToControlSeq(c: Int) =
    if (48 <= c && c <= 57) Number(List(c.toChar.toString().toInt))
    else if (c === 91) LeftSquareBracket
    else if (c === 59) SemiColon
    else if (65 <= c && c <= 90) CapitalLetter(c.toChar)
    else Unknown(c)

  type ControlSequence = List[ControlSeqenceChar]

  def removeCharAt(s: String, index: Int) =
    s.substring(0, index) + s.substring(index + 1)

  @tailrec
  def rightPad(s: String, n: Int): String =
    if (s.length() < n) rightPad(s + " ", n)
    else s

  def insertCharAt(s: String, char: Char, index: Int) = {
    val padded = rightPad(s, index)
    padded.substring(0, index) + char + padded.substring(index)
  }

  @SuppressWarnings(Array("DisableSyntax.throw"))
  case class TerminalState(
    cursor: (Int, Int),
    content: Map[Int, String],
    keyBuffer: ControlSequence
  ) {
    if (cursor._1 < 1 || cursor._2 < 1) throw new Exception(s"Invalid cursor: $cursor")
  }

  object TerminalState {
    def empty: TerminalState =
      TerminalState((25, 1), Map.empty, List.empty) // We start at bottom of the screen
  }

  def parse(output: String)(implicit debugger: Debugger): TerminalState = {
    import debugger._

    debug("parser started...")(())

    def cursorLeft(cursor: (Int, Int), step: Int) = (cursor._1, cursor._2 - step)
    def cursorRight(cursor: (Int, Int), step: Int) = (cursor._1, cursor._2 + step)
    def modifyLine(content: Map[Int, String], cursor: (Int, Int), f: String => String) =
      (content + (cursor._1 -> content.getOrElse(cursor._1, ""))).map {
        case (index, value) if index === cursor._1 => index -> f(value)
        case kv                                    => kv
      }

    val terminalState = output
      .toCharArray()
      .foldLeft[TerminalState](TerminalState.empty) {
        case (state @ TerminalState(cursor, content, buffer), char) =>
          implicit val depth: Debugger.Depth = Debugger.Depth(buffer.length)

          debug(state.toString())(())
          char.toInt match {
            case 27 =>
              debug(s"escape seq start, content: $content")(state.copy(keyBuffer = List(Escape)))

            case c if buffer.nonEmpty =>
              (buffer :+ byteToControlSeq(c)) match {
                case List(Escape, LeftSquareBracket, Number(digits), CapitalLetter('D')) =>
                  val n = digits.foldLeft("")(_ + _).toInt
                  debug(s"cursor left by ${n}")(state.copy(cursor = cursorLeft(cursor, n), keyBuffer = List.empty))
                case List(Escape, LeftSquareBracket, Number(digits), CapitalLetter('C')) =>
                  val n = digits.foldLeft("")(_ + _).toInt
                  debug(s"cursor right by ${n}")(state.copy(cursor = cursorRight(cursor, n), keyBuffer = List.empty))
                case List(Escape, LeftSquareBracket, Number(List(0)), CapitalLetter('K')) =>
                  debug("clearline")(
                    TerminalState(cursor, modifyLine(content, cursor, _.substring(0, cursor._2 - 1)), List.empty)
                  )
                case List(Escape, LeftSquareBracket, Number(digits1), SemiColon, Number(digits2), CapitalLetter('H')) =>
                  val row = digits1.foldLeft("")(_ + _).toInt
                  val column = digits2.foldLeft("")(_ + _).toInt
                  debug(s"move to ($row,$column)")(state.copy(cursor = (row, column), keyBuffer = List.empty))
                case seq =>
                  (buffer.last, byteToControlSeq(c)) match {
                    case (Number(digits1), Number(digits2)) =>
                      debug("next digit")(state.copy(keyBuffer = buffer.dropRight(1) :+ Number(digits1 ++ digits2)))
                    case (_, Unknown(c)) => debug(s"Unknow byte in sequence: $c")(state.copy(keyBuffer = List.empty))
                    case _               => debug(s"next in seq: $c")(state.copy(keyBuffer = seq))
                  }
              }

            case 127 =>
              debug("backspace")(
                TerminalState(
                  cursorLeft(cursor, 1),
                  modifyLine(content, cursor, removeCharAt(_, cursor._2 - 1 - 1)),
                  buffer
                )
              )
            case c if 32 <= c && c <= 255 && c =!= 127 =>
              debug(s"char: $char")(
                TerminalState(
                  cursorRight(cursor, 1),
                  modifyLine(content, cursor, insertCharAt(_, char, cursor._2 - 1)),
                  buffer
                )
              )
            case c =>
              debug(s"unknown: $c")(state.copy(keyBuffer = List.empty))
          }
      }

    terminalState.copy(content = terminalState.content.filter({ case (_, line) => line.nonEmpty }))
  }
}
