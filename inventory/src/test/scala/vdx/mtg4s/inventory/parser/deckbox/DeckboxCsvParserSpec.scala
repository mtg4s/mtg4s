package vdx.mtg4s.inventory.parser.deckbox

import cats.data.Chain
import cats.data.NonEmptyList
import cats.effect.IO
import monocle.Getter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vdx.mtg4s._
import vdx.mtg4s.inventory.InventoryItem
import vdx.mtg4s.inventory.parser.Parser.ParsingError

import java.util.UUID

class DeckboxCsvParserSpec extends AnyWordSpec with Matchers {
  case class Card(name: String, id: MtgJsonId)

  val primevalTitan = Card("Primeval Titan", MtgJsonId(UUID.randomUUID()))

  val cardDB: CardDB[IO, Card] = new CardDB[IO, Card] {

    override def find(name: CardName, set: Set): IO[Option[Card]] =
      IO.pure(
        Option(primevalTitan).filter(_.name === name)
      )

  }

  implicit val idGetter: Getter[Card, MtgJsonId] = _.id

  val parser = DeckboxCsvParser[IO, Card](cardDB)

  "Decbox parser" should {
    "return a parser error when the given string is not a valid csv" in {
      val csv = """
        |header
        |this not the csv we need
        """.stripMargin
      parser.parse(csv).unsafeRunSync() should be(Left(NonEmptyList.one(ParsingError("Error parsing line"))))
    }

    "return the parsed inventory when the given csv is valid" in {
      val csv =
        """
        | Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price
        | 4,0,Primeval Titan,Iconic Masters,183,Mint,English,,,,,,,,
        """.stripMargin

      parser.parse(csv).unsafeRunSync() should be(Right(Chain.one(InventoryItem(primevalTitan.id, 4))))
    }
  }
}
