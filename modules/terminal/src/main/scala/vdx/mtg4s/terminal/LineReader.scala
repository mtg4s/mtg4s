package vdx.mtg4s.terminal

import cats.Show
import cats.data.Chain
import cats.effect.Sync
import cats.instances.int._
import cats.instances.string._
import cats.instances.unit._
import cats.kernel.Eq
import cats.syntax.eq._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import vdx.mtg4s.terminal.LineReader.{AutoCompletionConfig, AutoCompletionSource}
//import cats.Eval

trait InputReader

trait LineReader[F[_]] {
  def readLine(prompt: String): F[String]
  def readLine[Repr: Show: Eq](prompt: String, autocomplete: AutoCompletionSource[Repr])(
    implicit cfg: AutoCompletionConfig
  ): F[(String, Option[Repr])]
}

object LineReader {
  trait AutoCompletionSource[Repr] {
    def candidates(fragment: String): List[(String, Repr)]
  }

  case class AutoCompletionConfig(
    maxCandidates: Int
  )
  // object LineReaderConfig {
  //   sealed trait AutoCompletionDirection
  //   case object Up extends AutoCompletionDirection
  //   case object Down extends AutoCompletionDirection
  // }

  // def const[A, B](b: B): A => B = _ => b
  // def tap[A, B](a: A)(f: A => B): A = f.andThen(const(a))(a)

  implicit val defaultAutoCompletionConfig: AutoCompletionConfig = AutoCompletionConfig(5)

  def apply[F[_]: Sync](terminal: Terminal): LineReader[F] = new LineReader[F] {
    import TerminalControl._

    type ByteSeq = Chain[Int]

    case class LineReaderState[Repr](
      keys: Chain[ByteSeq],
      column: Int,
      input: String,
      selected: Option[(Int, String, Repr)]
    )
    object LineReaderState {
      def empty[Repr]: LineReaderState[Repr] = apply(Chain.empty, 0, "", None)

      implicit class LineReaderStateOps[Repr](state: LineReaderState[Repr]) {
        def moveColumnBy(n: Int): LineReaderState[Repr] = state.copy(column = state.column + n)
        def prependKeys(keys: ByteSeq): LineReaderState[Repr] = state.copy(keys = Chain.one(keys) ++ state.keys)
        def withInput(input: String): LineReaderState[Repr] = state.copy(input = input)
        def clearSelection: LineReaderState[Repr] = state.copy(selected = None)
        def select(index: Int, input: String, selected: Repr): LineReaderState[Repr] =
          state.copy(selected = Option((index, input, selected)))
        def result: (String, Option[Repr]) = state.input -> state.selected.map(_._3)
      }
    }

    private val writer = terminal.writer()
    private val reader = terminal.reader()

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
              val currentRow = terminal.getCursorPosition()._1
              // handle keypress events that affects the input string
              // redraw input string
              // handle kepyress events that affects completion (e.g. UP/DOWN/TAB)
              // redraw completions
              // redraw input string if necessary (after TAB completion)
              val updatedState = keyPress(
                currentRow,
                prompt.length() + 1
              )(state, byteSeq)

              runCompletion(autocomplete, prompt, currentRow)(updatedState)
            }
            .result
        )

    // private[this] def updateInputState[Repr](promptRow: Int, promptLength: Int): Eval[LineReaderState[Repr]] = ???
    // private[this] def updateCompletionState[Repr](): Eval[LineReaderState[Repr]] = ???

    private[this] def readSequence(s: Chain[Int]): Chain[Int] = s match {
      case Chain(27)     => readSequence(s :+ reader.readchar())
      case Chain(27, 91) => readSequence(s :+ reader.readchar())
      case l             => l
    }

    private[this] def write(s: String): Unit = {
      writer.write(s)
      terminal.flush()
    }

    private[this] def runCompletion[Repr: Show: Eq](
      autocomplete: Option[(AutoCompletionConfig, AutoCompletionSource[Repr])],
      prompt: String,
      currentRow: Int
    )(
      state: LineReaderState[Repr]
    ): LineReaderState[Repr] =
      autocomplete.fold(state) {
        case (cfg, ac) =>
          // handle keypress
          val newState = state.keys.headOption match {
            case Some(Chain(9)) =>
              val s = state.copy(
                input = state.selected.fold(state.input)(_._2),
                column = state.selected.fold(state.column)(_._2.length())
              )
              // redraw input line
              write(
                move(currentRow, prompt.length() + 1) +
                  clearLine() +
                  s.input
              )
              s
            case _ => state
          }

          // print candidates
          val candidates = ac.candidates(newState.input)
          printCompletionCandidates(candidates, prompt, cfg)
          newState.copy(
            selected = newState.selected.flatMap { selected =>
              candidates.zipWithIndex.find {
                case ((str, repr), _) => str === selected._2 && repr === selected._3
              }
            }.fold(candidates.headOption.map { case (str, repr) => (1, str, repr) }) {
              case ((str, repr), index) => Option((index, str, repr))
            }
          )
      }

    private[this] def printCompletionCandidates[Repr: Show](
      candidates: List[(String, Repr)],
      prompt: String,
      cfg: AutoCompletionConfig
    ): Unit = {
      val (row, col) = terminal.getCursorPosition()
      (1 to cfg.maxCandidates).foreach(i => write(move(row - i, 1) + clearLine))
      candidates.take(cfg.maxCandidates).reverse.zipWithIndex.foreach {
        case ((_, candidate), index) =>
          write(move((row - 1) - index, prompt.length() + 1) ++ candidate.show)
      }
      write(move(row, col))
    }

    private[this] def keyPress[Repr](
      row: Int,
      promptLength: Int
    )(state: LineReaderState[Repr], byteSeq: ByteSeq): LineReaderState[Repr] = {
      val newState = state.prependKeys(byteSeq)

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
          write(back())
          newState.moveColumnBy(-1)

        case Chain(27, 91, 67) if newState.column < newState.input.length =>
          write(forward())
          newState.moveColumnBy(1)

        //   case Chain(27, 91, 70) => end()
        //   case Chain(5) => end()

        //   case Chain(27, 91, 72) => home()
        //   case Chain(1) => home()

        case Chain(c) if ((32 <= c && c <= 126) || 127 < c) =>
          val (_front, _back) = newState.input.splitAt(newState.column)
          write(clearLine() + c.toChar + _back + move(row, promptLength + newState.column + 1))
          newState.moveColumnBy(1).withInput(_front + c.toChar + _back)

        case Chain(127) if newState.column > 0 => // Backspace
          val (_front, _back) = newState.input.splitAt(newState.column)
          val newFront = _front.dropRight(1)
          write(back() + clearLine() + _back + move(row, promptLength + newState.column - 1))
          newState.moveColumnBy(-1).withInput(newFront + _back)

        case Chain(27, 91, 51) => // Delete
          val (_front, _back) = newState.input.splitAt(newState.column)
          val newBack = _back.drop(1)
          write(clearLine() + newBack + move(row, promptLength + newState.column))
          newState.withInput(_front + newBack)

        case _ => newState
      }
    }
  }
}
