package vdx.mtg4s.terminal

trait Debugger {
  def debug[A](message: String)(a: A)(implicit depth: Debugger.Depth): A
}

object Debugger {
  final case class Depth(value: Int) extends AnyVal

  implicit val depth: Depth = Depth(0)

  def printlnDebugger(enabled: Boolean): Debugger = new Debugger {
    def debug[A](message: String)(a: A)(implicit depth: Debugger.Depth): A = {
      if (enabled) {
        val tabs = (1 to depth.value).map(_ => "\t").mkString
        println(tabs + message)
      }
      a
    }
  }
}
