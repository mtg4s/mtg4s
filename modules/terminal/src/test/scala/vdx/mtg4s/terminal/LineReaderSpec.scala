package vdx.mtg4s.terminal

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.util.Try

import cats.effect.IO
import cats.instances.list._
import cats.instances.string._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vdx.mtg4s.terminal.AutoCompletionConfig.Down
import vdx.mtg4s.terminal.AutoCompletionSource
import vdx.mtg4s.terminal.TerminalHelper.TerminalState

class LineReaderSpec extends AnyWordSpec with Matchers {
  val backspace: Int = 127
  val carriageReturn: Int = 13
  val escape: Int = 27
  val leftSquareBracket: Int = 91
  val tab: Int = 9
  val cursorUp: List[Int] = List(escape, leftSquareBracket, 65)
  val cursorDown: List[Int] = List(escape, leftSquareBracket, 66)
  val cursorRight: List[Int] = List(escape, leftSquareBracket, 67)
  val cursorLeft: List[Int] = List(escape, leftSquareBracket, 68)
  val delete: List[Int] = List(escape, leftSquareBracket, 51)

  def reader(keys: List[Int])(implicit debugger: Debugger): (TestTerminal, LineReader[IO], String) = {
    val terminal = TestTerminal(keys)

    (terminal, LineReader(terminal), "prompt > ")
  }

  val autocomplete: AutoCompletionSource[String] =
    str =>
      List(
        "foo",
        "bar",
        "baz",
        "foobar",
        "foobarbaz"
      ).filter(_.startsWith(str)).map(s => s -> s)

  implicit val debugger = Debugger.printlnDebugger(false)

  "LineReader.readline" when {

    "only simple characters are typed" should {

      "return an empty string if enter is the first character" in {
        val (_, lineReader, _) = reader(List(carriageReturn))
        lineReader.readLine("This is a prompt").unsafeRunSync should be("")
      }

      "return the typed text" in {
        val (_, lineReader, prompt) = reader(strToChars("This is a test!\r"))
        lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
      }

      "display the text on the terminal" in {
        val text = "This is a test!"
        val (term, lineReader, prompt) = reader(strToChars(s"${text}\r"))

        lineReader.readLine(prompt).unsafeRunSync()

        TerminalHelper.parse(term.output) should be(
          TerminalState(
            25 -> (prompt.length() + text.length() + 1),
            HashMap(25 -> s"${prompt}This is a test!"),
            List.empty,
            List.empty
          )
        )
      }
    }

    "characters are added in the middle of the string (after moving the cursor)" should {
      "insert a character after the chursor when moving the cursor back" in {
        val (_, lineReader, prompt) = reader(
          strToChars("This is a est!") ++ cursorLeft ++ cursorLeft ++ cursorLeft ++ cursorLeft ++
            List('t'.toInt, carriageReturn)
        )

        lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
      }

      "insert a character after the chursor when moving the cursor back then forward" in {
        val (_, lineReader, prompt) = reader(
          strToChars("This is a est!") ++ repeat(cursorLeft, 2) ++ cursorRight ++ repeat(cursorLeft, 3) ++
            List('t'.toInt, carriageReturn)
        )
        lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
      }

      "display the text on the terminal properly when moving the cursor back then forward" in {
        val text = "This is a est!"
        val (term, lineReader, prompt) = reader(
          strToChars(text) ++ repeat(cursorLeft, 2) ++ cursorRight ++ repeat(cursorLeft, 3) ++
            List('t'.toInt, carriageReturn)
        )
        lineReader.readLine(prompt).unsafeRunSync()
        TerminalHelper.parse(term.output) should be(
          TerminalState(25 -> (prompt.length + 12), HashMap(25 -> s"${prompt}This is a test!"), List.empty, List.empty)
        )

      }
    }

    "one ore more characters are deleted" should {
      "delete the character before the cursor when pressing backspace" in {
        val (_, lineReader, prompt) =
          reader(strToChars("This is a teest!") ++ repeat(cursorLeft, 4) ++ List(backspace, carriageReturn))
        lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
      }

      "update the output after deleting a charactere with backspace" in {
        val text = "This is a teest!"
        val (term, lineReader, prompt) =
          reader(strToChars(text) ++ repeat(cursorLeft, 4) ++ List(backspace, carriageReturn))
        lineReader.readLine(prompt).unsafeRunSync()
        TerminalHelper.parse(term.output) should be(
          TerminalState(
            25 -> (prompt.length() + text.length() + 1 - 4 - 1),
            Map(25 -> s"${prompt}This is a test!"),
            List.empty,
            List.empty
          )
        )
      }
    }

    "one or more characters are deleted with the delete key" should {
      "delete the character after the cursor when pressing DELETE" in {
        val (_, lineReader, prompt) = reader(
          strToChars("This is a tyest!") ++ repeat(cursorLeft, 5) ++
            delete ++ List(carriageReturn)
        )
        lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
      }

      "properly update the terminal when pressing DELETE" in {
        val (term, lineReader, prompt) = reader(
          strToChars("This is a tyest!") ++ repeat(cursorLeft, 5) ++
            delete ++ List(carriageReturn)
        )
        lineReader.readLine(prompt).unsafeRunSync()
        TerminalHelper.parse(term.output) should be(
          TerminalState(25 -> (prompt.length + 12), HashMap(25 -> s"${prompt}This is a test!"), List.empty, List.empty)
        )
      }
    }

    "a complex combination of keys is pressed" should {
      "update the terminal properly after multiple keypress" in {
        val (term, lineReader, prompt) = reader(
          strToChars("This is a tyest!") ++
            repeat(cursorLeft, 5) ++
            delete ++
            repeat(cursorRight, 3) ++
            strToChars(", no reallyyy") ++
            repeat(List(backspace), 2) ++
            List(carriageReturn)
        )

        lineReader.readLine(prompt).unsafeRunSync()
        TerminalHelper.parse(term.output) should be(
          TerminalState(
            25 -> (26 + prompt.length()),
            HashMap(25 -> s"${prompt}This is a test, no really!"),
            List.empty,
            List.empty
          )
        )
      }
    }

    "autocompletion is provided" should {
      "display available completions" in {
        implicit val debugger = Debugger.printlnDebugger(false)
        val (term, lineReader, prompt) = reader(strToChars("foo") ++ List(carriageReturn))

        lineReader.readLine(prompt, autocomplete).unsafeRunSync()

        val output = Try(TerminalHelper.parse(term.output)(debugger)).toEither

        output should be(
          Right(
            TerminalState(
              25 -> (3 + 1 + prompt.length()),
              HashMap(
                22 -> (repeat(" ", prompt.length()) + "foo"),
                23 -> (repeat(" ", prompt.length()) + "foobar"),
                24 -> (repeat(" ", prompt.length()) + "foobarbaz"),
                25 -> s"${prompt}foo"
              ),
              List.empty,
              List.empty
            )
          )
        )
      }

      "select the first candidate when the TAB key is pressed" in {
        val (term, lineReader, prompt) = reader(strToChars("f") ++ List(tab, carriageReturn))

        lineReader.readLine(prompt, autocomplete).unsafeRunSync()

        val output = Try(TerminalHelper.parse(term.output)(debugger)).toEither

        output should be(
          Right(
            TerminalState(
              25 -> (3 + 1 + prompt.length()),
              HashMap(
                22 -> (repeat(" ", prompt.length()) + "foo"),
                23 -> (repeat(" ", prompt.length()) + "foobar"),
                24 -> (repeat(" ", prompt.length()) + "foobarbaz"),
                25 -> s"${prompt}foo"
              ),
              List.empty,
              List.empty
            )
          )
        )
      }

      "select a candidate with the cursor down key" in {
        val (term, lineReader, prompt) = reader(strToChars("f") ++ cursorDown ++ List(tab, carriageReturn))

        lineReader.readLine(prompt, autocomplete).unsafeRunSync()

        val output = Try(TerminalHelper.parse(term.output)(debugger)).toEither

        output should be(
          Right(
            TerminalState(
              25 -> (6 + 1 + prompt.length()),
              HashMap(
                23 -> (repeat(" ", prompt.length()) + "foobar"),
                24 -> (repeat(" ", prompt.length()) + "foobarbaz"),
                25 -> s"${prompt}foobar"
              ),
              List.empty,
              List.empty
            )
          )
        )
      }

      "select a candidate with the cursor up key" in {
        val (term, lineReader, prompt) =
          reader(strToChars("f") ++ cursorDown ++ cursorDown ++ cursorUp ++ List(tab, carriageReturn))

        lineReader.readLine(prompt, autocomplete).unsafeRunSync()

        val output = Try(TerminalHelper.parse(term.output)(debugger)).toEither

        output should be(
          Right(
            TerminalState(
              25 -> (6 + 1 + prompt.length()),
              HashMap(
                23 -> (repeat(" ", prompt.length()) + "foobar"),
                24 -> (repeat(" ", prompt.length()) + "foobarbaz"),
                25 -> s"${prompt}foobar"
              ),
              List.empty,
              List.empty
            )
          )
        )
      }

      "should append new characters properly after selecting a candidate" in {
        val (term, lineReader, prompt) =
          reader(strToChars("f") ++ List(tab) ++ strToChars(" bar") ++ List(carriageReturn))

        lineReader.readLine(prompt, autocomplete).unsafeRunSync()

        val output = Try(TerminalHelper.parse(term.output)(debugger)).toEither

        output should be(
          Right(
            TerminalState(
              25 -> (7 + 1 + prompt.length()),
              HashMap(
                25 -> s"${prompt}foo bar"
              ),
              List.empty,
              List.empty
            )
          )
        )
      }

      "should return the result when the input is not modified" in {
        val (_, lineReader, prompt) =
          reader(strToChars("f") ++ List(tab) ++ cursorDown ++ cursorDown ++ List(carriageReturn))

        val (_, maybeResult) = lineReader.readLine(prompt, autocomplete).unsafeRunSync()
        maybeResult should be(Some("foo"))
      }

      "should not return the result when the input is modified" in {
        val (_, lineReader, prompt) =
          reader(strToChars("f") ++ List(tab) ++ strToChars("b") ++ List(carriageReturn))

        val (_, maybeResult) = lineReader.readLine(prompt, autocomplete).unsafeRunSync()
        maybeResult should be(None)
      }

      "display the completions below the prompt in configured" in {
        val (term, lineReader, prompt) = reader(strToChars("foo") ++ List(carriageReturn))

        implicit val acConfig: AutoCompletionConfig[String] =
          AutoCompletionConfig
            .defaultAutoCompletionConfig[String]
            .copy(
              direction = Down
            )

        lineReader.readLine(prompt, autocomplete).unsafeRunSync()

        val output = Try(TerminalHelper.parse(term.output)(debugger)).toEither

        output should be(
          Right(
            TerminalState(
              25 -> (3 + 1 + prompt.length()),
              HashMap(
                25 -> s"${prompt}foo",
                26 -> (repeat(" ", prompt.length()) + "foo"),
                27 -> (repeat(" ", prompt.length()) + "foobar"),
                28 -> (repeat(" ", prompt.length()) + "foobarbaz")
              ),
              List.empty,
              List.empty
            )
          )
        )
      }
    }

    "autocompletion is provided and it is configured to be strict" should {
      implicit val acConfig: AutoCompletionConfig[String] =
        AutoCompletionConfig
          .defaultAutoCompletionConfig[String]
          .copy(
            maxCandidates = 10,
            strict = true
          )

      "not provide a result while a completion candidate is not selected" in {
        val (_, lineReader, prompt) =
          reader(strToChars("f") ++ List(carriageReturn))

        val result = lineReader.readLine(prompt, autocomplete).attempt.unsafeRunSync()
        result should be(Left(TestTerminal.endOfInputException))
      }

      "provide a result while a completion candidate is selected" in {
        val (_, lineReader, prompt) =
          reader(strToChars("f") ++ List(tab, carriageReturn))

        val result = lineReader.readLine[String](prompt, autocomplete).unsafeRunSync()
        result should be(("foo", Some("foo")))
      }

      "provide the correct result while a completion candidate is selected after hitting enter" in {
        val (term, lineReader, prompt) =
          reader(
            strToChars("f") ++ List(carriageReturn) ++ strToChars("oob") ++ cursorDown ++ List(tab, carriageReturn)
          )

        val result = lineReader.readLine(prompt, autocomplete).unsafeRunSync()
        result should be(("foobarbaz", Some("foobarbaz")))

        val output = Try(TerminalHelper.parse(term.output)(debugger)).toEither
        output should be(
          Right(
            TerminalState(
              25 -> (9 + 1 + prompt.length()),
              HashMap(
                24 -> (repeat(" ", prompt.length()) + "foobarbaz"),
                25 -> s"${prompt}foobarbaz"
              ),
              List.empty,
              List.empty
            )
          )
        )

      }
    }

    "an onResultChange handler is provided" should {
      def config(logs: mutable.ListBuffer[Option[String]]) =
        AutoCompletionConfig.defaultAutoCompletionConfig.copy(
          maxCandidates = 10,
          strict = true,
          onResultChange = (maybeResult: Option[String], _) => logs.addOne(maybeResult)
        )

      "call the handler with the selected completion each time a completions is selected with tab key" in {
        val (_, lineReader, prompt) =
          reader(strToChars("f") ++ List(tab) ++ cursorDown ++ List(tab, carriageReturn))

        val logs = mutable.ListBuffer.empty[Option[String]]
        implicit val cfg = config(logs)
        lineReader.readLine(prompt, autocomplete).unsafeRunSync()

        logs.toList should be(List(Some("foo"), Some("foobar")))

      }

      "Call the handler when a completion is unselected" in {
        val (_, lineReader, prompt) =
          reader(
            strToChars("f") ++ List(tab) ++ strToChars("b") ++ List(tab) ++ strToChars("b") ++ List(tab, carriageReturn)
          )

        val logs = mutable.ListBuffer.empty[Option[String]]
        implicit val cfg = config(logs)
        lineReader.readLine(prompt, autocomplete).unsafeRunSync()

        logs.toList should be(List(Some("foo"), None, Some("foobar"), None, Some("foobarbaz")))
      }
    }
  }
}
