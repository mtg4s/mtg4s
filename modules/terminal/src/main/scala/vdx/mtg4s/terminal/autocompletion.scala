package vdx.mtg4s.terminal

trait AutoCompletionSource[Repr] {
  def candidates(fragment: String): List[(String, Repr)]
}

case class AutoCompletionConfig(
  maxCandidates: Int
)

object AutoCompletionConfig {
  implicit val defaultAutoCompletionConfig: AutoCompletionConfig = AutoCompletionConfig(5)
}
