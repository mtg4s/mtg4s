package vdx.mtg4s.terminal

import cats.data.{Chain, StateT}
import cats.effect.Sync
import cats.instances.int._
import cats.instances.string._
import cats.syntax.eq._
import cats.syntax.flatMap._
import cats.syntax.functor._
import vdx.mtg4s.terminal.LineReader.Autocomplete

trait InputReader

trait LineReader[F[_]] {
  def readLine(prompt: String): F[String] = readLine(prompt, None)
  def readLine(prompt: String, autocomplete: Autocomplete[F]): F[String] = readLine(prompt, Option(autocomplete))
  def readLine(prompt: String, autocomplete: Option[Autocomplete[F]]): F[String]
}

object LineReader {
  trait Autocomplete[F[_]] {
    def candidates(fragment: String): F[List[String]]
  }

  def apply[F[_]: Sync](terminal: Terminal): LineReader[F] = new LineReader[F] {
    import TerminalControl._

    type ByteSeq = Chain[Int]
    case class LineReaderState(keys: Chain[ByteSeq], column: Int, input: String)
    object LineReaderState {
      implicit class LineReaderStateOps(state: LineReaderState) {
        def moveColumn(by: Int) = state.copy(column = state.column + by)
        def tupled: (LineReaderState, String) = state -> state.input
        def tupledF: F[(LineReaderState, String)] = Sync[F].pure(tupled)
      }
    }
    type InputState = StateT[F, LineReaderState, String]

    private val writer = terminal.writer()
    private val reader = terminal.reader()

    def readLine(prompt: String, autocomplete: Option[Autocomplete[F]]): F[String] =
      Sync[F].delay(write(prompt)) >>
        Sync[F].delay(LazyList.continually(reader.readchar())) >>= { keys =>
        keys
          .takeWhile(_ =!= 13)
          .map(Chain.one)
          .map(readSequence)
          .foldLeft[StateT[F, LineReaderState, String]](StateT.empty) { (state, byteSeq) =>
            state >>
              keyPress(terminal.getCursorPosition()._1, prompt.length() + 1)(byteSeq) >>
              runCompletion(autocomplete, prompt)
          }
          .runA(LineReaderState(Chain.empty, 0, ""))
      }

    private[this] def readSequence(s: Chain[Int]): Chain[Int] =
      s match {
        case Chain(27)     => readSequence(s :+ reader.readchar())
        case Chain(27, 91) => readSequence(s :+ reader.readchar())
        case l             => l
      }

    private[this] def write(s: String): Unit = {
      writer.write(s)
      terminal.flush()
    }

    private[this] def runCompletion(autocomplete: Option[Autocomplete[F]], prompt: String): InputState =
      StateT { state =>
        autocomplete.fold(state.tupledF)(ac =>
          ac.candidates(state.input)
            .flatTap(printCompletionCandidates(_, prompt))
            .as(state.tupled)
        )
      }

    private[this] def printCompletionCandidates(candidates: List[String], prompt: String): F[Unit] =
      Sync[F].delay {
        val (row, col) = terminal.getCursorPosition()
        (1 to 5).foreach(i => write(move(row - 1 - i, 1) + clearLine))
        candidates.take(5).reverse.zipWithIndex.foreach {
          case (candidate, index) =>
            write(move((row - 1) - index, prompt.length() + 1) ++ candidate)
        }
        write(move(row, col))
      }

    private[this] def keyPress(
      row: Int,
      promptLength: Int
    )(byteSeq: ByteSeq): InputState =
      StateT { state =>
//        println(s"state is $state")
        val keys = Chain.one(byteSeq) ++ state.keys
        val newState = state.copy(keys = keys)

        // def home() = {
        //   write(move(row, promptLength))
        //   (history, 0, oldStr)
        // }

        // def end() = {
        //   val cursor = oldStr.length()
        //   write(move(row, promptLength + cursor))
        //   (history, cursor, oldStr)
        // }

        byteSeq match {
          case Chain(27, 91, 68) if state.column > 0 =>
            write(back())
            newState.moveColumn(-1).tupledF

          case Chain(27, 91, 67) if state.column < state.input.length =>
            write(forward())
            newState.moveColumn(1).tupledF

          //   case Chain(27, 91, 70) => end()
          //   case Chain(5) => end()

          //   case Chain(27, 91, 72) => home()
          //   case Chain(1) => home()

          case Chain(c) if ((32 <= c && c <= 126) || 127 < c) =>
            val (_front, _back) = state.input.splitAt(state.column)
            write(clearLine() + c.toChar + _back + move(row, promptLength + state.column + 1))
            newState.moveColumn(1).copy(input = _front + c.toChar + _back).tupledF

          case Chain(127) if state.column > 0 => // Backspace
            val (_front, _back) = state.input.splitAt(state.column)
            val newFront = _front.dropRight(1)
            write(back() + clearLine() + _back + move(row, promptLength + state.column - 1))
            newState.moveColumn(-1).copy(input = newFront + _back).tupledF

          case Chain(27, 91, 51) => // Delete
            val (_front, _back) = state.input.splitAt(state.column)
            val newBack = _back.drop(1)
            write(clearLine() + newBack + move(row, promptLength + state.column))
            newState.copy(input = _front + newBack).tupledF

          case _ => newState.tupledF
        }
      }
  }
}
