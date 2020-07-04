package vdx.mtg4s.terminal

trait AutoCompletionSource[Repr] {
  def candidates(fragment: String): List[(String, Repr)]
}

case class AutoCompletionConfig[Repr](
  maxCandidates: Int,
  strict: Boolean,
  onResultChange: (Option[Repr], String => Unit) => Unit
)

object AutoCompletionConfig {
  implicit def defaultAutoCompletionConfig[Repr]: AutoCompletionConfig[Repr] =
    AutoCompletionConfig(
      maxCandidates = 5,
      strict = false,
      onResultChange = (_, _) => ()
    )
}
