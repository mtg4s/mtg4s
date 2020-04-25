package vdx.mtg4s.terminal

import cats.data.Chain
import cats.instances.int._
import cats.syntax.eq._

trait LineReader {
  def readLine(prompt: String): String
}

object LineReader {
  def apply(terminal: Terminal): LineReader = new LineReader {
    import TerminalControl._

    type ByteSeq = Chain[Int]
    type LineReaderState = (Chain[ByteSeq], Int, String)

    private val writer = terminal.writer()
    private val reader = terminal.reader()

    def readLine(prompt: String): String = {
      LazyList
        .continually(reader.readchar())
        .takeWhile(_ =!= 13)
        .map(chr => Chain.one(chr))
        .map(readSequence)
        .foldLeft[LineReaderState]((Chain.empty[Chain[Int]], 0, ""))(
          keyPress(
            terminal.getCursorPosition()._1,
            prompt.length() + 1
          )
        )
        ._3
    }

    private[this] def readSequence(s: Chain[Int]): Chain[Int] = s match {
      case Chain(27)     => readSequence(s :+ reader.readchar())
      case Chain(27, 91) => readSequence(s :+ reader.readchar())
      case l             => l
    }

    private def write(s: String): Unit = {
      writer.write(s)
      terminal.flush()
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
