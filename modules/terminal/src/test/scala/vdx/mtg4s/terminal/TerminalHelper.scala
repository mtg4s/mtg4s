package vdx.mtg4s.terminal

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

  def insertCharAt(s: String, char: Char, index: Int) =
    s.substring(0, index) + char + s.substring(index)

  def parse(output: String, prompt: String)(implicit debugger: Debugger): (String, Int, ControlSequence) = {
    import debugger._

    output
      .toCharArray()
      .foldLeft[(String, Int, ControlSequence)](("", prompt.length + 1, List.empty)) {
        case ((result, cursor, buffer), char) =>
          implicit val depth: Debugger.Depth = Debugger.Depth(buffer.length)
          debug(result + " | " + cursor + " | " + buffer + " | " + char)(())
          char.toInt match {
            case 27 =>
              debug(s"escape seq start, result: $result")((result, cursor, List(Escape)))

            case c if buffer.nonEmpty =>
              (buffer :+ byteToControlSeq(c)) match {
                case List(Escape, LeftSquareBracket, Number(digits), CapitalLetter('D')) =>
                  val n = digits.foldLeft("")(_ + _).toInt
                  debug(s"cursor left by ${n}")((result, cursor - n, List.empty))
                case List(Escape, LeftSquareBracket, Number(digits), CapitalLetter('C')) =>
                  val n = digits.foldLeft("")(_ + _).toInt
                  debug(s"cursor right by ${n}")((result, cursor + n, List.empty))
                case List(Escape, LeftSquareBracket, Number(List(0)), CapitalLetter('K')) =>
                  debug("clearline")((result.substring(0, cursor - prompt.length - 1), cursor, List.empty))
                case List(Escape, LeftSquareBracket, Number(_), SemiColon, Number(digits2), CapitalLetter('H')) =>
                  val column = digits2.foldLeft("")(_ + _).toInt
                  debug(s"move to col $column")((result, column, List.empty))
                case seq =>
                  (buffer.last, byteToControlSeq(c)) match {
                    case (Number(digits1), Number(digits2)) =>
                      debug("next digit")((result, cursor, buffer.dropRight(1) :+ Number(digits1 ++ digits2)))
                    case (_, Unknown(_)) => debug("end")((result, cursor, List.empty))
                    case _               => debug(s"next in seq: $c")((result, cursor, seq))
                  }
              }

            case 127 =>
              debug("backspace")((removeCharAt(result, cursor - 1 - prompt.length() - 1), cursor - 1, buffer))
            case c if 32 <= c && c <= 255 && c =!= 127 =>
              debug(s"char: $char")((insertCharAt(result, char, cursor - prompt.length() - 1), cursor + 1, buffer))
            case c =>
              debug(s"unknown: $c")((result, cursor, List.empty))
          }
      }
  }
}
