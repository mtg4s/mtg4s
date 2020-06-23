package vdx.mtg4s.terminal

import scala.collection.immutable.HashMap
import scala.util.Try

import cats.effect.IO
import cats.instances.list._
import cats.instances.string._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vdx.mtg4s.terminal.LineReader.AutoCompletionSource
import vdx.mtg4s.terminal.TerminalHelper.TerminalState

class LineReaderSpec extends AnyWordSpec with Matchers {
  val backspace: Int = 127
  val carriageReturn: Int = 13
  val escape: Int = 27
  val leftSquareBracket: Int = 91
  val tab: Int = 9
  val leftArrow: List[Int] = List(escape, leftSquareBracket, 68)
  val rightArrow: List[Int] = List(escape, leftSquareBracket, 67)
  val delete: List[Int] = List(escape, leftSquareBracket, 51)

  def reader(keys: List[Int])(implicit debugger: Debugger): (TestTerminal, LineReader[IO], String) = {
    val terminal = TestTerminal(keys)

    (terminal, LineReader(terminal), "prompt > ")
  }
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
            List.empty
          )
        )
      }
    }

    "characters are added in the middle of the string (after moving the cursor)" should {
      "insert a character after the chursor when moving the cursor back" in {
        val (_, lineReader, prompt) = reader(
          strToChars("This is a est!") ++ leftArrow ++ leftArrow ++ leftArrow ++ leftArrow ++
            List('t'.toInt, carriageReturn)
        )

        lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
      }

      "insert a character after the chursor when moving the cursor back then forward" in {
        val (_, lineReader, prompt) = reader(
          strToChars("This is a est!") ++ repeat(leftArrow, 2) ++ rightArrow ++ repeat(leftArrow, 3) ++
            List('t'.toInt, carriageReturn)
        )
        lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
      }

      "display the text on the terminal properly when moving the cursor back then forward" in {
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
    }

    "one ore more characters are deleted" should {
      "delete the character before the cursor when pressing backspace" in {
        val (_, lineReader, prompt) =
          reader(strToChars("This is a teest!") ++ repeat(leftArrow, 4) ++ List(backspace, carriageReturn))
        lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
      }

      "update the output after deleting a charactere with backspace" in {
        val text = "This is a teest!"
        val (term, lineReader, prompt) =
          reader(strToChars(text) ++ repeat(leftArrow, 4) ++ List(backspace, carriageReturn))
        lineReader.readLine(prompt).unsafeRunSync()
        TerminalHelper.parse(term.output) should be(
          TerminalState(
            25 -> (prompt.length() + text.length() + 1 - 4 - 1),
            Map(25 -> s"${prompt}This is a test!"),
            List.empty
          )
        )
      }
    }

    "one or more characters are deleted with the delete key" should {
      "delete the character after the cursor when pressing DELETE" in {
        val (_, lineReader, prompt) = reader(
          strToChars("This is a tyest!") ++ repeat(leftArrow, 5) ++
            delete ++ List(carriageReturn)
        )
        lineReader.readLine(prompt).unsafeRunSync should be("This is a test!")
      }

      "properly update the terminal when pressing DELETE" in {
        val (term, lineReader, prompt) = reader(
          strToChars("This is a tyest!") ++ repeat(leftArrow, 5) ++
            delete ++ List(carriageReturn)
        )
        lineReader.readLine(prompt).unsafeRunSync()
        TerminalHelper.parse(term.output) should be(
          TerminalState(25 -> (prompt.length + 12), HashMap(25 -> s"${prompt}This is a test!"), List.empty)
        )
      }
    }

    "a complex combination of keys is pressed" should {
      "update the terminal properly after multiple keypress" in {
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
    }

    "autocompletion is provided" should {

      val autocomplete: AutoCompletionSource[String] = str =>
        List(
          "foo",
          "bar",
          "baz",
          "foobar",
          "foobarbaz"
        ).filter(_.startsWith(str)).map(s => s -> s)

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
              List.empty
            )
          )
        )
      }

      "select the first candidate when the TAB key is pressed" in {
        implicit val debugger = Debugger.printlnDebugger(false)
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
              List.empty
            )
          )
        )
      }

      "should append new characters properly after selecting a candidate" in {
        implicit val debugger = Debugger.printlnDebugger(false)
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
              List.empty
            )
          )
        )
      }
    }
  }
}
