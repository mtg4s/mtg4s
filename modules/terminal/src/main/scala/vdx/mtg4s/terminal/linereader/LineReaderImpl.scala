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
      )(implicit cfg: AutoCompletionConfig[Repr]): F[(String, Option[Repr])] =
        readLine(prompt, Option(cfg -> autocomplete))

      private def readLine[Repr: Show: Eq](
        prompt: String,
        autocomplete: Option[(AutoCompletionConfig[Repr], AutoCompletionSource[Repr])]
      ): F[(String, Option[Repr])] =
        Sync[F].delay(write(prompt)) >>
          readInput(LineReaderState.empty, Env(terminal.getCursorPosition()._1, prompt, autocomplete))

      private def readInput[Repr: Show: Eq](
        state: LineReaderState[Repr],
        env: Env[Repr]
      ): F[(String, Option[Repr])] =
        Sync[F]
          .delay(
            LazyList
              .continually(readSequence(Chain.one(reader.readchar())))
              .takeWhile(_ =!= Chain(13))
              .foldLeft(state) { (state, byteSeq) =>
                (for {
                  out1 <- handleKeypress[Repr](byteSeq)
                  out2 <- AutoCompletion.updateCompletions[Repr]
                  _    <- StateUpdate.now(write(out1 + out2))
                } yield ())
                  .runS(env, state)
                  .value
              }
          )
          .flatMap { state =>
            env.autocomplete.fold(Sync[F].pure(state.result)) { ac =>
              if (ac._1.strict && state.result._2.isEmpty) readInput(state, env)
              else Sync[F].pure(state.result)
            }
          }

      private[this] def handleKeypress[Repr: Eq](byteSeq: Chain[Int]): StateUpdate[Repr, String] =
        StateUpdate { (env, state) =>
          val newState = state.prependKeys(byteSeq)
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

          byteSeq match {
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
              newState
                .moveColumnBy(1)
                .withInput(_front + c.toChar + _back, env, write) -> (clearLine() + c.toChar + _back + move(
                env.currentRow,
                readerStart + newState.column + 1
              ))

            case Chain(127) if newState.column > 0 => // Backspace
              val (_front, _back) = newState.input.splitAt(newState.column)
              val newFront = _front.dropRight(1)
              newState.moveColumnBy(-1).withInput(newFront + _back, env, write) -> (back() + clearLine() + _back + move(
                env.currentRow,
                readerStart + newState.column - 1
              ))

            case Chain(27, 91, 51) => // Delete
              val (_front, _back) = newState.input.splitAt(newState.column)
              val newBack = _back.drop(1)
              newState.withInput(_front + newBack, env, write) -> (clearLine() + newBack + move(
                env.currentRow,
                readerStart + newState.column
              ))

            // Completion related cases
            case Chain(9) => // Tab
              val s = newState.copy(
                input = state.selectedCompletion.fold(state.input)(_._2),
                column = state.selectedCompletion.fold(state.column)(_._2.length()),
                completionResult = newState.selectedCompletion.map(_._3)
              )
              env.autocomplete.foreach {
                case (config, _) => config.onResultChange(s.completionResult, write)
              }
              s -> (move(env.currentRow, readerStart) + clearLine() + s.input)

            case Chain(27, 91, 65) => // Up
              newState.copy(selectedCompletion = findCompletionOrElseCurrent(state, -1, env)) -> ""

            case Chain(27, 91, 66) => // Down
              newState.copy(selectedCompletion = findCompletionOrElseCurrent(state, 1, env)) -> ""

            case _ => newState -> ""
          }
        }
    }

  private def findCompletionOrElseCurrent[Repr](
    state: LineReaderState[Repr],
    offset: Int,
    env: Env[Repr]
  ): Option[(Int, String, Repr)] =
    state.selectedCompletion.flatMap {
      case old @ (index, _, _) =>
        env.autocomplete.map {
          case (config, source) =>
            source
              .candidates(state.input)
              .take(config.maxCandidates)
              .zipWithIndex
              .map { case ((input, repr), i) => (i, input, repr) }
              .applyOrElse(index + offset, (_: Int) => old)
        }
    }
}
