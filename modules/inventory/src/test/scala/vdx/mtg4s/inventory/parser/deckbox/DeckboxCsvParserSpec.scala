package vdx.mtg4s.inventory.parser.deckbox

import java.util.UUID

import cats.data.{Chain, NonEmptyList}
import cats.effect.IO
import kantan.csv._
import kantan.csv.generic._
import kantan.csv.ops._
import monocle.Getter
import org.scalacheck.Prop
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.claimant.Claim
import vdx.mtg4s.TestCardDB.{defaultCards, Card}
import vdx.mtg4s._
import vdx.mtg4s.inventory.Inventory
import vdx.mtg4s.inventory.Inventory.PaperCard
import vdx.mtg4s.inventory.parser.Parser.{CardNotFoundError, ParsingError}
import vdx.mtg4s.inventory.parser.deckbox.Generators._

@SuppressWarnings(Array("scalafix:DisableSyntax.==")) // For nice Claimant messages
class DeckboxCsvParserSpec extends AnyWordSpec with Matchers with Checkers {

  implicit val idGetter: Getter[Card, UUID] = _.id

  val cardDB = TestCardDB[IO]()

  val parser = DeckboxCsvParser[IO, Card, UUID](cardDB)

  val deckboxCsvConfig = rfc.withHeader(
    "Count",
    "Tradelist Count",
    "Name",
    "Edition",
    "Card Number",
    "Condition",
    "Language",
    "Foil",
    "Signed",
    "Artist Proof",
    "Altered Art",
    "Misprint",
    "Promo",
    "Textless",
    "My Price"
  )

  private[this] def toCsv(row: DeckboxRow) = {
    val stringw = new java.io.StringWriter()
    stringw.asCsvWriter[DeckboxRow](deckboxCsvConfig).write(row).close()

    stringw.toString()
  }

  "Decbox parser" should {
    "return a parser error when the given string is not a valid csv" in {
      val csv = """
        |header
        |this not the csv we need
        """.stripMargin
      parser.parse(csv).unsafeRunSync() should be(
        Left(
          NonEmptyList.one(
            ParsingError("Error parsing/decoding line 2: TypeError: 'this not the csv we need' is not a valid Int")
          )
        )
      )
    }

    "return the parsed inventory when csv is valid" in {
      check(
        Prop.forAll(validDeckboxRow) { row =>
          val parserResult = parser.parse(toCsv(row)).unsafeRunSync()
          val cardFromDb = defaultCards(s"${row.name} - ${row.edition}")

          Claim(
            parserResult.fold(errors => fail(s"Right value was expected but got Left: $errors"), identity) == Inventory(
              Chain.one(PaperCard(cardFromDb.id, row.count, false))
            )
          )
        }
      )
    }

    "return error when card name is not matching" in {
      check(
        Prop.forAll(nonExistentNameRow) { row =>
          val parserResult = parser.parse(toCsv(row)).unsafeRunSync()

          Claim(
            parserResult.swap.getOrElse(fail()) == NonEmptyList.one(
              CardNotFoundError(s"Cannot find card in the database: ${row.name} (${row.edition}) at line 2")
            )
          )
        }
      )
    }

    "return error when set is not matching" in {
      check(
        Prop.forAll(nonExistentSetRow) { row =>
          val parserResult = parser.parse(toCsv(row)).unsafeRunSync()

          Claim(
            parserResult.swap.getOrElse(fail("Left value was expected but got Right")) == NonEmptyList.one(
              CardNotFoundError(s"Cannot find card in the database: ${row.name} (${row.edition}) at line 2")
            )
          )
        }
      )
    }

  }
}
