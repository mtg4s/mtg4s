package vdx.mtg4s.terminal.linereader

import cats.data.{Chain, Reader, StateT}
import cats.instances.option._
import cats.syntax.apply._
import vdx.mtg4s.terminal.{AutoCompletionConfig, AutoCompletionSource}

private[linereader] case class LineReaderState[Repr](
  keys: Chain[ByteSeq],
  column: Int,
  input: String,
  selectedCompletion: Option[(Int, String, Repr)],
  completionResult: Option[Repr]
)

private[linereader] object LineReaderState {
  type EnvReader[Repr, A] = Reader[Env[Repr], A]

  type StateUpdate[Repr] = StateT[EnvReader[Repr, *], LineReaderState[Repr], String]
  object StateUpdate {
    def apply[Repr](f: (LineReaderState[Repr], Env[Repr]) => (LineReaderState[Repr], String)): StateUpdate[Repr] =
      StateT(s => Reader(e => f(s, e)))
  }

  case class Env[Repr](
    currentRow: Int,
    prompt: String,
    byteSeq: ByteSeq,
    autocomplete: Option[(AutoCompletionConfig[Repr], AutoCompletionSource[Repr])]
  )

  def empty[Repr]: LineReaderState[Repr] = apply(Chain.empty, 0, "", None, None)

  implicit class LineReaderStateOps[Repr](state: LineReaderState[Repr]) {
    def moveColumnBy(n: Int): LineReaderState[Repr] = state.copy(column = state.column + n)
    def prependKeys(keys: ByteSeq): LineReaderState[Repr] = state.copy(keys = Chain.one(keys) ++ state.keys)
    def withInput(input: String, env: Env[Repr], write: String => Unit): LineReaderState[Repr] = {
      (
        env.autocomplete,
        state.completionResult
      ).mapN { case ((config, _), _) => config.onResultChange(None, write) }
      state.copy(input = input, completionResult = None)
    }
    def selectCompletion(index: Int, input: String, selected: Repr): LineReaderState[Repr] =
      state.copy(selectedCompletion = Option((index, input, selected)))
    def selectResult(result: Repr) = state.copy(completionResult = Option(result))
    def result: (String, Option[Repr]) = state.input -> state.completionResult
  }
}
