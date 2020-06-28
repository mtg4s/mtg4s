package vdx.mtg4s.terminal.linereader

import cats.Show
import cats.instances.list._
import cats.instances.string._
import cats.kernel.Eq
import cats.syntax.eq._
import cats.syntax.foldable._
import cats.syntax.show._
import vdx.mtg4s.terminal.AutoCompletionConfig
import vdx.mtg4s.terminal.TerminalControl._
import vdx.mtg4s.terminal.linereader.LineReaderState.StateUpdate

private[linereader] object AutoCompletion {

  def updateCompletions[Repr: Show: Eq]: StateUpdate[Repr] =
    StateUpdate { (state, env) =>
      env.autocomplete.fold(state -> "") {
        case (cfg, ac) =>
          val candidates = ac.candidates(state.input)
          val out = printCompletionCandidates(candidates, env.prompt, env.currentRow, cfg)
          state.copy(
            selected = state.selected.flatMap { selected =>
              candidates.zipWithIndex.find {
                case ((str, repr), _) => str === selected._2 && repr === selected._3
              }
            }.fold(candidates.headOption.map { case (str, repr) => (1, str, repr) }) {
              case ((str, repr), index) => Option((index, str, repr))
            }
          ) -> out
      }
    }

  def printCompletionCandidates[Repr: Show](
    candidates: List[(String, Repr)],
    prompt: String,
    row: Int,
    cfg: AutoCompletionConfig
  ): String = {
    candidates
      .take(cfg.maxCandidates)
      .reverse
      .zipWithIndex
      .foldLeft(savePos() + clearCompletionLines(row, cfg)) {
        case (o, ((_, candidate), index)) =>
          o + move((row - 1) - index, prompt.length() + 1) ++ candidate.show
      } + restorePos()
  }

  def clearCompletionLines(row: Int, cfg: AutoCompletionConfig): String =
    (1 to cfg.maxCandidates).toList.foldMap(i => move(row - i, 1) + clearLine())
}
