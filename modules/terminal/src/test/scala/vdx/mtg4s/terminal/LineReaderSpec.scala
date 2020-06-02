package vdx.mtg4s.terminal

import scala.collection.immutable.HashMap

import cats.effect.IO
import cats.instances.list._
import cats.kernel.Semigroup
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import vdx.mtg4s.terminal.TerminalHelper.TerminalState

class LineReaderSpec extends AnyFlatSpec with Matchers {
  val backspace: Int = 127
  val carriageReturn: Int = 13
  val escape: Int = 27
  val leftSquareBracket: Int = 91
  val leftArrow: List[Int] = List(escape, leftSquareBracket, 68)
  val rightArrow: List[Int] = List(escape, leftSquareBracket, 67)
  val delete: List[Int] = List(escape, leftSquareBracket, 51)

  @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
  class TestTerminal(keys: List[Int]) extends Terminal {
    private val _debugger = Debugger.printlnDebugger(false)

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
          case Nil     => fail("There aren't more keys")
        }
      }
    }

    def flush(): Unit = {
      _output = _output + _writerBuffer
      _writerBuffer = ""
    }

    def getCursorPosition(): (Terminal.Row, Terminal.Column) = {
      TerminalHelper.parse(_output + _writerBuffer)(_debugger).cursor
    }

    def output: String = _output
  }

  def reader(keys: List[Int]): (TestTerminal, LineReader[IO], String) = {
    val terminal = new TestTerminal(keys)

    (terminal, LineReader(terminal), "prompt > ")
  }

  def strToChars(s: String): List[Int] = s.toCharArray.map(_.toInt).toList

  def repeat[A](a: A, n: Int)(implicit S: Semigroup[A]): A =
    if (n <= 1) a
    else S.combine(a, repeat(a, n - 1))

  implicit val debugger = Debugger.printlnDebugger(false)

  "LineReader.readline" should "return an empty string if enter is the first character" in {
    val (_, lineReader, _) = reader(List(carriageReturn))
    lineReader.readLine("This is a prompt").unsafeRunSync should be("")
  }

  // Simple Ascii

  it should "return the typed ascii text" in {
    val (_, lineReader, prompt) = reader(strToChars("This is a test!\r"))
    lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
  }

  it should "display the ascii text on the terminal" in {
    val text = "This is a test!"
    val (term, lineReader, prompt) = reader(strToChars(s"${text}\r"))

    lineReader.readLine(prompt).unsafeRunSync()

    TerminalHelper.parse(term.output) should be(
      TerminalState(25 -> (prompt.length() + text.length() + 1), HashMap(25 -> s"${prompt}This is a test!"), List.empty)
    )
  }

  // Insert char in the middle of the text (left and right arrows)

  it should "insert a character after the chursor after moving the cursor back" in {
    val (_, lineReader, prompt) = reader(
      strToChars("This is a est!") ++ leftArrow ++ leftArrow ++ leftArrow ++ leftArrow ++
        List('t'.toInt, carriageReturn)
    )

    lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
  }

  it should "insert a character after the chursor after moving the cursor back then forward" in {
    val (_, lineReader, prompt) = reader(
      strToChars("This is a est!") ++ repeat(leftArrow, 2) ++ rightArrow ++ repeat(leftArrow, 3) ++
        List('t'.toInt, carriageReturn)
    )
    lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
  }

  it should "display the text on the terminal properly after moving the cursor back then forward" in {
    val text = "This is a est!"
    val (term, lineReader, prompt) = reader(
      strToChars(text) ++ repeat(leftArrow, 2) ++ rightArrow ++ repeat(leftArrow, 3) ++
        List('t'.toInt, carriageReturn)
    )
    lineReader.readLine(prompt).unsafeRunSync()
    TerminalHelper.parse(term.output) should be(
      TerminalState(25 -> (prompt.length + 12), HashMap(25 -> s"${prompt}This is a test!"), List.empty)
    )

  }

  // Delete char with backspace

  it should "delete the character before the cursor when pressing backspace" in {
    val (_, lineReader, prompt) =
      reader(strToChars("This is a teest!") ++ repeat(leftArrow, 4) ++ List(backspace, carriageReturn))
    lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
  }

  it should "update the output after deleting a charactere with backspace" in {
    val text = "This is a teest!"
    val (term, lineReader, prompt) = reader(strToChars(text) ++ repeat(leftArrow, 4) ++ List(backspace, carriageReturn))
    lineReader.readLine(prompt).unsafeRunSync()
    TerminalHelper.parse(term.output) should be(
      TerminalState(
        25 -> (prompt.length() + text.length() + 1 - 4 - 1),
        Map(25 -> s"${prompt}This is a test!"),
        List.empty
      )
    )
  }

  // Delete character with delete key

  it should "delete the character after the cursor when pressing DELETE" in {
    val (_, lineReader, prompt) = reader(
      strToChars("This is a tyest!") ++ repeat(leftArrow, 5) ++
        delete ++ List(carriageReturn)
    )
    lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
  }

  it should "properly update the terminal when pressing DELETE" in {
    val (term, lineReader, prompt) = reader(
      strToChars("This is a tyest!") ++ repeat(leftArrow, 5) ++
        delete ++ List(carriageReturn)
    )
    lineReader.readLine(prompt).unsafeRunSync()
    TerminalHelper.parse(term.output) should be(
      TerminalState(25 -> (prompt.length + 12), HashMap(25 -> s"${prompt}This is a test!"), List.empty)
    )
  }

  // Multiple keys

  it should "update the terminal properly after multiple keypress" in {
    val (term, lineReader, prompt) = reader(
      strToChars("This is a tyest!") ++
        repeat(leftArrow, 5) ++
        delete ++
        repeat(rightArrow, 3) ++
        strToChars(", no reallyyy") ++
        repeat(List(backspace), 2) ++
        List(carriageReturn)
    )

    lineReader.readLine(prompt).unsafeRunSync()
    TerminalHelper.parse(term.output) should be(
      TerminalState(25 -> (26 + prompt.length()), HashMap(25 -> s"${prompt}This is a test, no really!"), List.empty)
    )
  }

  // "LineReader.readline with autocomplete" should "display available completions" in {
  //   val (_, lineReader, prompt) = reader(strToChars("foo") ++ List(carriageReturn))

  //   val autocomplete: IO[String => List[String]] = IO.delay { str =>
  //     List(
  //       "foo",
  //       "bar",
  //       "baz",
  //       "foobar"
  //     ).filter(_.startsWith(str))
  //   }

  //   lineReader.readLine(prompt, autocomplete).unsafeRunSync()
  // }
}
