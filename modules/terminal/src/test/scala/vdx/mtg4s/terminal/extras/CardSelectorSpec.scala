package vdx.mtg4s.terminal.extras

import cats.Id
import cats.arrow.FunctionK
import cats.effect.IO
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vdx.mtg4s.TestCardDB
import vdx.mtg4s.TestCardDB.Card
import vdx.mtg4s.terminal.{strToChars, Console, Debugger, LineReader, TestTerminal}

class CardSelectorSpec extends AnyWordSpec with Matchers {
  def initConsole(keys: List[Int]): (TestTerminal, LineReader[IO], Console[IO]) = {
    implicit val debugger: Debugger = Debugger.printlnDebugger(false)
    val terminal = TestTerminal(keys)
    val lineReader = LineReader[IO](terminal)
    val console: Console[IO] = Console[IO](terminal, lineReader)

    (terminal, lineReader, console)
  }

  def runIO[A](io: IO[A]): Id[A] = io.unsafeRunSync()
  val run: FunctionK[IO, Id] = FunctionK.lift[IO, Id](runIO)

  "CardSelector" when {
    "there's a single match" should {
      "return the matching card" in {
        val (_, _, console) = initConsole(strToChars("Lightning") ++ List(9, 13))

        implicit val c: Console[IO] = console

        val cardDb = TestCardDB[IO]()
        val selector = CardSelector[IO, Card](run)

        selector.run(cardDb).unsafeRunSync() should be(Option(TestCardDB.defaultCards("Lightning Bolt - Masters 25")))
      }
    }

    "there are multiple matches" should {
      "return the first match by default" in {
        val (_, _, console) = initConsole(strToChars("Primeval") ++ List(9, 13))

        implicit val c: Console[IO] = console

        val cardDb = TestCardDB[IO]()
        val selector = CardSelector[IO, Card](run)

        selector.run(cardDb).unsafeRunSync() should be(
          Option(TestCardDB.defaultCards("Primeval Titan - Iconic Masters"))
        )
      }
      "return the selected matching card" in {
        val (_, _, console) = initConsole(strToChars("Primeval") ++ List(24, 24) ++ List(9, 13))

        implicit val c: Console[IO] = console

        val cardDb = TestCardDB[IO]()
        val selector = CardSelector[IO, Card](run)

        selector.run(cardDb).unsafeRunSync() should be(
          Option(TestCardDB.defaultCards("Primeval Titan - Iconic Masters"))
        )
      }
    }
  }
}
