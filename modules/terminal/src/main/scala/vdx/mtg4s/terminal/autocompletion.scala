package vdx.mtg4s.terminal

import cats.Eq
import vdx.mtg4s.terminal.AutoCompletionConfig.Direction

trait AutoCompletionSource[Repr] {
  def candidates(fragment: String): List[(String, Repr)]
}

case class AutoCompletionConfig[Repr](
  maxCandidates: Int,
  strict: Boolean,
  direction: Direction,
  onResultChange: (Option[Repr], String => Unit) => Unit
)

object AutoCompletionConfig {
  sealed trait Direction
  case object Up extends Direction
  case object Down extends Direction

  object Direction {
    implicit val eq: Eq[Direction] = Eq.fromUniversalEquals
  }

  implicit def defaultAutoCompletionConfig[Repr]: AutoCompletionConfig[Repr] =
    AutoCompletionConfig(
      maxCandidates = 5,
      strict = false,
      direction = Up,
      onResultChange = (_, _) => ()
    )
}
