package vdx.mtg4s.terminal.linereader

import cats.Show
import cats.data.Chain
import cats.effect.Sync
import cats.instances.int._
import cats.instances.unit._
import cats.kernel.Eq
import cats.syntax.eq._
import cats.syntax.flatMap._
import cats.syntax.functor._
import vdx.mtg4s.terminal.TerminalControl._
import vdx.mtg4s.terminal._
import vdx.mtg4s.terminal.linereader.LineReaderState._

object LineReaderImpl {
  private[terminal] def apply[F[_]: Sync](terminal: Terminal): LineReader[F] =
    new LineReader[F] {

      type ByteSeq = Chain[Int]

      private val writer = terminal.writer()
      private val reader = terminal.reader()

      private[this] def readSequence(s: Chain[Int]): Chain[Int] = s match {
        case Chain(27)     => readSequence(s :+ reader.readchar())
        case Chain(27, 91) => readSequence(s :+ reader.readchar())
        case l             => l
      }

      private[this] def write(s: String): Unit = {
        writer.write(s)
        terminal.flush()
      }

      def readLine(prompt: String): F[String] = readLine[Unit](prompt, None).map(_._1)
      def readLine[Repr: Show: Eq](
        prompt: String,
        autocomplete: AutoCompletionSource[Repr]
      )(implicit cfg: AutoCompletionConfig): F[(String, Option[Repr])] =
        readLine(prompt, Option(cfg -> autocomplete))

      private def readLine[Repr: Show: Eq](
        prompt: String,
        autocomplete: Option[(AutoCompletionConfig, AutoCompletionSource[Repr])]
      ): F[(String, Option[Repr])] =
        Sync[F].delay(write(prompt)) >>
          Sync[F].delay(
            LazyList
              .continually(reader.readchar())
              .takeWhile(_ =!= 13)
              .map(Chain.one)
              .map(readSequence)
              .foldLeft(LineReaderState.empty[Repr]) { (state, byteSeq) =>
                val env = Env(terminal.getCursorPosition()._1, prompt, byteSeq, autocomplete)

                val (newState, out) =
                  (for {
                    out1 <- handleKeypress
                    out2 <- AutoCompletion.updateCompletions[Repr]
                  } yield out1 + out2).run(state).run(env)
                write(out)
                newState
              }
              .result
          )

      private[this] def handleKeypress[Repr]: StateUpdate[Repr] =
        StateUpdate { (state, env) =>
          val newState = state.prependKeys(env.byteSeq)
          val readerStart = env.prompt.length + 1

          // def home() = {
          //   write(move(row, promptLength))
          //   (history, 0, oldStr)
          // }

          // def end() = {
          //   val cursor = oldStr.length()
          //   write(move(row, promptLength + cursor))
          //   (history, cursor, oldStr)
          // }

          env.byteSeq match {
            case Chain(27, 91, 68) if newState.column > 0 =>
              newState.moveColumnBy(-1) -> back()

            case Chain(27, 91, 67) if newState.column < newState.input.length =>
              newState.moveColumnBy(1) -> forward()

            //   case Chain(27, 91, 70) => end()
            //   case Chain(5) => end()

            //   case Chain(27, 91, 72) => home()
            //   case Chain(1) => home()

            case Chain(c) if ((32 <= c && c <= 126) || 127 < c) =>
              val (_front, _back) = newState.input.splitAt(newState.column)
              newState.moveColumnBy(1).withInput(_front + c.toChar + _back) -> (clearLine() + c.toChar + _back + move(
                env.currentRow,
                readerStart + newState.column + 1
              ))

            case Chain(127) if newState.column > 0 => // Backspace
              val (_front, _back) = newState.input.splitAt(newState.column)
              val newFront = _front.dropRight(1)
              newState.moveColumnBy(-1).withInput(newFront + _back) -> (back() + clearLine() + _back + move(
                env.currentRow,
                readerStart + newState.column - 1
              ))

            case Chain(27, 91, 51) => // Delete
              val (_front, _back) = newState.input.splitAt(newState.column)
              val newBack = _back.drop(1)
              newState.withInput(_front + newBack) -> (clearLine() + newBack + move(
                env.currentRow,
                readerStart + newState.column
              ))

            case Chain(9) =>
              val s = newState.copy(
                input = state.selected.fold(state.input)(_._2),
                column = state.selected.fold(state.column)(_._2.length())
              )
              s -> (move(env.currentRow, readerStart) + clearLine() + s.input)

            case _ => newState -> ""
          }
        }
    }

}
