package vdx.mtg4s.terminal

trait TestTerminal extends Terminal {
  def output: String
}

@SuppressWarnings(Array("scalafix:DisableSyntax.var", "scalafix:DisableSyntax.throw"))
object TestTerminal {
  def apply(keys: List[Int])(implicit debugger: Debugger): TestTerminal =
    new TestTerminal {
      private[this] var _keys = keys
      private[this] var _output = ""
      private[this] var _writerBuffer = ""

      def writer(): Terminal.Writer = new Terminal.Writer {
        def write(s: String): Unit = _writerBuffer = _writerBuffer + s
      }

      def reader(): Terminal.Reader = new Terminal.Reader {
        def readchar(): Int = {
          flush()
          _keys match {
            case x :: xs => { _keys = xs; x }
            case Nil     => throw new RuntimeException("There aren't more keys")
          }
        }
      }

      def flush(): Unit = {
        _output = _output + _writerBuffer
        _writerBuffer = ""
      }

      def getCursorPosition(): (Terminal.Row, Terminal.Column) = {
        TerminalHelper.parse(_output + _writerBuffer)(debugger).cursor
      }

      def output: String = _output

      def getHeight(): Int = 25
    }
}
