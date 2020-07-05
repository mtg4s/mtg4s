package vdx.mtg4s.terminal.linereader

import cats.Show
import cats.instances.int._
import cats.instances.list._
import cats.instances.string._
import cats.kernel.Eq
import cats.syntax.eq._
import cats.syntax.foldable._
import cats.syntax.show._
import vdx.mtg4s.terminal.AutoCompletionConfig
import vdx.mtg4s.terminal.AutoCompletionConfig.Up
import vdx.mtg4s.terminal.TerminalControl._
import vdx.mtg4s.terminal.linereader.LineReaderState.StateUpdate

private[linereader] object AutoCompletion {

  def updateCompletions[Repr: Show: Eq]: StateUpdate[Repr] =
    StateUpdate { (state, env) =>
      env.autocomplete.fold(state -> "") {
        case (cfg, ac) =>
          val completions = ac.candidates(state.input).take(cfg.maxCandidates)
          val newState = state.copy(
            selectedCompletion = findMatchingCandidate(completions, state.selectedCompletion)
          )

          val out = printCompletionCandidates(
            completions,
            env.prompt,
            env.currentRow,
            cfg,
            newState.selectedCompletion.map(_._1)
          )
          newState -> out
      }
    }

  private[this] def findMatchingCandidate[Repr: Eq](
    completions: List[(String, Repr)],
    maybeSelected: Option[(Int, String, Repr)]
  ): Option[(Int, String, Repr)] = {
    maybeSelected
      .flatMap(selected =>
        completions.zipWithIndex.find {
          case ((str, repr), _) => str === selected._2 && repr === selected._3
        }
      )
      .fold(completions.headOption.map { case (str, repr) => (0, str, repr) }) {
        case ((str, repr), index) => Option((index, str, repr))
      }
  }

  private[this] def printCompletionCandidates[Repr: Show](
    completions: List[(String, Repr)],
    prompt: String,
    row: Int,
    cfg: AutoCompletionConfig[Repr],
    selected: Option[Int]
  ): String = {
    completions.zipWithIndex
      .foldLeft(savePos() + clearCompletionLines(row, cfg)) {
        case (o, ((_, candidate), index)) =>
          o + move(completionRow(cfg, row, index, completions.length), prompt.length() + 1) +
            (
              if (selected.filter(_ === index).isDefined) bold() + candidate.show + sgrReset()
              else candidate.show
            )
      } + restorePos()
  }

  private[this] def completionRow[Repr](
    config: AutoCompletionConfig[Repr],
    inputRow: Int,
    index: Int,
    completions: Int
  ) =
    (if (config.direction === Up) inputRow - completions else inputRow + 1) + index

  private[this] def clearCompletionLines[Repr](row: Int, cfg: AutoCompletionConfig[Repr]): String =
    (1 to cfg.maxCandidates).toList.foldMap(i => move(row + (if (cfg.direction === Up) -i else i), 1) + clearLine())
}
