package vdx.mtg4s.terminal

trait AutoCompletionSource[Repr] {
  def candidates(fragment: String): List[(String, Repr)]
}

case class AutoCompletionConfig(
  maxCandidates: Int,
  strict: Boolean,
)

object AutoCompletionConfig {
  implicit val defaultAutoCompletionConfig: AutoCompletionConfig = 
    AutoCompletionConfig(
      maxCandidates = 5,
      strict = false
    )
}
