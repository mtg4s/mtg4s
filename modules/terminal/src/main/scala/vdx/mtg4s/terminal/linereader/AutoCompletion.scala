package vdx.mtg4s.terminal.linereader

import cats.Show
import cats.instances.int._
import cats.instances.list._
import cats.instances.string._
import cats.instances.unit._
import cats.kernel.Eq
import cats.syntax.eq._
import cats.syntax.foldable._
import cats.syntax.show._
import vdx.mtg4s.terminal.AutoCompletionConfig
import vdx.mtg4s.terminal.AutoCompletionConfig.Up
import vdx.mtg4s.terminal.TerminalControl._
import vdx.mtg4s.terminal.linereader.LineReaderState.StateUpdate

private[linereader] object AutoCompletion {

  def updateCompletions[Repr: Show: Eq]: StateUpdate[Repr, String] =
    StateUpdate.lift { (env, state) =>
      env.autocomplete.fold(StateUpdate.pure[Repr, String]("")) {
        case (cfg, ac) =>
          val completions = ac.candidates(state.input).take(cfg.maxCandidates)

          for {
            _    <- updateSelectedCompletion(completions)
            out1 <- clearCompletionLines()
            out2 <- printCompletionCandidates(completions)
          } yield savePos() + out1 + out2 + restorePos()
        // val newState = state.copy(
        //   selectedCompletion = findMatchingCandidate(completions, state.selectedCompletion)
        // )

        // val out = printCompletionCandidates(
        //   completions,
        //   env.prompt,
        //   env.currentRow,
        //   cfg,
        //   newState.selectedCompletion.map(_._1)
        // )
        // newState -> out
      }
    }

  private[this] def updateSelectedCompletion[Repr: Eq](candidates: List[(String, Repr)]): StateUpdate[Repr, Unit] =
    StateUpdate.modify((_, state) =>
      state.copy(
        selectedCompletion = state.selectedCompletion
          .flatMap(selected =>
            candidates.zipWithIndex.find {
              case ((str, repr), _) => str === selected._2 && repr === selected._3
            }
          )
          .fold(candidates.headOption.map { case (str, repr) => (0, str, repr) }) {
            case ((str, repr), index) => Option((index, str, repr))
          }
      )
    )

  private[this] def printCompletionCandidates[Repr: Show](
    completions: List[(String, Repr)]
  ): StateUpdate[Repr, String] =
    StateUpdate.inspect { (env, state) =>
      env.autocomplete.fold("") {
        case (config, _) =>
          completions.zipWithIndex
            .foldLeft("") {
              case (o, ((_, candidate), index)) =>
                o + move(completionRow(config, env.currentRow, index, completions.length), env.prompt.length() + 1) +
                  (
                    if (state.selectedCompletion.filter(_._1 === index).isDefined) bold() + candidate.show + sgrReset()
                    else candidate.show
                  )
            }
      }
    }

  private[this] def completionRow[Repr](
    config: AutoCompletionConfig[Repr],
    inputRow: Int,
    index: Int,
    completions: Int
  ) =
    (if (config.direction === Up) inputRow - completions else inputRow + 1) + index

  private[this] def clearCompletionLines[Repr](): StateUpdate[Repr, String] =
    StateUpdate.ask.map { env =>
      env.autocomplete.fold("") {
        case (config, _) =>
          (1 to config.maxCandidates).toList
            .foldMap(i => move(env.currentRow + (if (config.direction === Up) -i else i), 1) + clearLine())
      }
    }
}
