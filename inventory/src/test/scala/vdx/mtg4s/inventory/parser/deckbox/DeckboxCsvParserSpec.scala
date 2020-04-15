package vdx.mtg4s.inventory.parser.deckbox

import cats.data.Chain
import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.eq._
import kantan.csv._
import kantan.csv.generic._
import kantan.csv.ops._
import monocle.Getter
import org.scalacheck.Prop
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.claimant.Claim
import vdx.mtg4s.MtgSet.SetName
import vdx.mtg4s._
import vdx.mtg4s.inventory.InventoryItem
import vdx.mtg4s.inventory.parser.Parser.CardNotFoundError
import vdx.mtg4s.inventory.parser.Parser.ParsingError
import vdx.mtg4s.inventory.parser.deckbox.Generators._

import java.util.UUID
@SuppressWarnings(Array("scalafix:DisableSyntax.==")) // For nice Claimant messages
class DeckboxCsvParserSpec extends AnyWordSpec with Matchers with Checkers {
  case class Card(name: String, set: SetName, id: MtgJsonId)

  val cards: Map[String, Card] = Map(
    "Primeval Titan" -> Card("Primeval Titan", SetName("Iconic Masters"), MtgJsonId(UUID.randomUUID())),
    "Snapcaster Mage" -> Card("Snapcaster Mage", SetName("Innistrad"), MtgJsonId(UUID.randomUUID())),
    "Lightning Bolt" -> Card("Lightning Bolt", SetName("Masters 25"), MtgJsonId(UUID.randomUUID())),
    "Karn, the Great Creator" -> Card(
      "Karn, the Great Creator",
      SetName("War of the Spark"),
      MtgJsonId(UUID.randomUUID())
    )
  )

  val cardDB: CardDB[IO, Card] = new CardDB[IO, Card] {

    override def findByNameAndSetCode(name: vdx.mtg4s.CardName, setCode: MtgSet.SetCode): IO[Option[Card]] = ???

    override def findByNameAndSetName(name: CardName, set: SetName): IO[Option[Card]] =
      IO.pure(cards.get(name).filter(_.set === set))
  }

  implicit val idGetter: Getter[Card, MtgJsonId] = _.id

  val parser = DeckboxCsvParser[IO, Card](cardDB)

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
          val cardFromDb = cards(row.name)

          Claim(
            parserResult.getOrElse(fail("Right value was expected but got Left")) == Chain.one(
              InventoryItem(cardFromDb.id, row.count)
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
