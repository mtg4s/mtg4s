package vdx.mtg4s.terminal.linereader

import cats.data.{Chain, Reader, StateT}
import vdx.mtg4s.terminal.{AutoCompletionConfig, AutoCompletionSource}

private[linereader] case class LineReaderState[Repr](
  keys: Chain[ByteSeq],
  column: Int,
  input: String,
  selected: Option[(Int, String, Repr)]
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
    autocomplete: Option[(AutoCompletionConfig, AutoCompletionSource[Repr])]
  )

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
