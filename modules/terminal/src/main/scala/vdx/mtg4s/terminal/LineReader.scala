package vdx.mtg4s.terminal

import cats.data.Chain
import cats.effect.Sync
import cats.instances.int._
import cats.syntax.eq._
import cats.syntax.flatMap._

trait InputReader

trait LineReader[F[_]] {
  def readLine(prompt: String): F[String] = readLine(prompt, None)
  def readLine(prompt: String, autocomplete: Option[String => List[String]]): F[String]
}

object LineReader {
  def const[A, B](b: B): A => B = _ => b

  def apply[F[_]: Sync](terminal: Terminal): LineReader[F] = new LineReader[F] {
    import TerminalControl._

    type ByteSeq = Chain[Int]
    type LineReaderState = (Chain[ByteSeq], Int, String)

    private val writer = terminal.writer()
    private val reader = terminal.reader()

    def readLine(prompt: String, autocomplete: Option[String => List[String]]): F[String] =
      Sync[F].delay(write(prompt)) >>
        Sync[F].delay(
          LazyList
            .continually(reader.readchar())
            .takeWhile(_ =!= 13)
            .map(Chain.one)
            .map(readSequence)
            .foldLeft[LineReaderState]((Chain.empty[Chain[Int]], 0, "")) { (state, byteSeq) =>
              runCompletion(autocomplete, prompt)(
                keyPress(
                  terminal.getCursorPosition()._1,
                  prompt.length() + 1
                )(state, byteSeq)
              )
            }
            ._3
        )

    private[this] def readSequence(s: Chain[Int]): Chain[Int] = s match {
      case Chain(27)     => readSequence(s :+ reader.readchar())
      case Chain(27, 91) => readSequence(s :+ reader.readchar())
      case l             => l
    }

    private[this] def write(s: String): Unit = {
      writer.write(s)
      terminal.flush()
    }

    private[this] def runCompletion(autocomplete: Option[String => List[String]], prompt: String)(
      state: LineReaderState
    ): LineReaderState =
      state match {
        case state @ (_, _, line) =>
          autocomplete.fold(())(ac => printCompletionCandidates(ac(line), prompt))
          state
      }

    private[this] def printCompletionCandidates(candidates: List[String], prompt: String): Unit = {
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
    )(state: LineReaderState, byteSeq: ByteSeq): LineReaderState = {
      val (oldHistory, oldCursor, oldStr) = state
      val history = Chain.one(byteSeq) ++ oldHistory

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
        case Chain(27, 91, 68) if oldCursor > 0 =>
          write(back())
          (history, oldCursor - 1, oldStr)

        case Chain(27, 91, 67) if oldCursor < oldStr.length =>
          write(forward())
          (history, oldCursor + 1, oldStr)

        //   case Chain(27, 91, 70) => end()
        //   case Chain(5) => end()

        //   case Chain(27, 91, 72) => home()
        //   case Chain(1) => home()

        case Chain(c) if ((32 <= c && c <= 126) || 127 < c) =>
          val (_front, _back) = oldStr.splitAt(oldCursor)
          write(clearLine() + c.toChar + _back + move(row, promptLength + oldCursor + 1))
          (history, oldCursor + 1, _front + c.toChar + _back)

        case Chain(127) if oldCursor > 0 => // Backspace
          val (_front, _back) = oldStr.splitAt(oldCursor)
          val newFront = _front.dropRight(1)
          write(back() + clearLine() + _back + move(row, promptLength + oldCursor - 1))
          (history, oldCursor - 1, newFront + _back)

        case Chain(27, 91, 51) => // Delete
          val (_front, _back) = oldStr.splitAt(oldCursor)
          val newBack = _back.drop(1)
          write(clearLine() + newBack + move(row, promptLength + oldCursor))
          (history, oldCursor, _front + newBack)

        case _ => (history, oldCursor, oldStr)
      }
    }
  }
}
