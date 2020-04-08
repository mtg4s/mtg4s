package vdx.mtg4s.inventory.parser.deckbox

import cats.syntax.apply._
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._
import vdx.mtg4s.Generators._

object Generators {
  case class DeckboxRow(
    count: Int,
    tradeListCount: Int,
    name: String,
    edition: String,
    cardNumber: Int,
    condition: String,
    language: String,
    foil: String,
    signed: String,
    artistProof: String,
    alteredArt: String,
    misprint: String,
    promo: String,
    textless: String,
    myPrice: Int
  )

  val validDeckboxRow: Gen[DeckboxRow] =
    validCards.flatMap {
      case (name, set) =>
        (
          Gen.choose(1, 10),
          Gen.choose(1, 10),
          Gen.const(name),
          Gen.oneOf(set),
          Gen.choose(1, 300),
          Gen.oneOf("Mint", "Near mint", "Good", "Played", "Heavily Played", "Poor"),
          Gen.oneOf("English", "German", "Italian"),
          Gen.oneOf("foil", ""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.choose(0, 60)
        ).mapN(DeckboxRow.apply)
    }

  val nonExistentNameRow: Gen[DeckboxRow] =
    validCards.flatMap {
      case (_, set) =>
        (
          Gen.choose(1, 10),
          Gen.choose(1, 10),
          Gen.oneOf("Godzilla", "Optimus Prime", "Darth Vader"),
          Gen.oneOf(set),
          Gen.choose(1, 300),
          Gen.oneOf("Mint", "Near mint", "Good", "Played", "Heavily Played", "Poor"),
          Gen.oneOf("English", "German", "Italian"),
          Gen.oneOf("foil", ""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.choose(0, 60)
        ).mapN(DeckboxRow.apply)
    }

  val nonExistentSetRow: Gen[DeckboxRow] =
    validCards.flatMap {
      case (name, _) =>
        (
          Gen.choose(1, 10),
          Gen.choose(1, 10),
          Gen.const(name),
          Gen.oneOf("Broccoli", "Soup", "Carrot"),
          Gen.choose(1, 300),
          Gen.oneOf("Mint", "Near mint", "Good", "Played", "Heavily Played", "Poor"),
          Gen.oneOf("English", "German", "Italian"),
          Gen.oneOf("foil", ""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.const(""),
          Gen.choose(0, 60)
        ).mapN(DeckboxRow.apply)
    }
}
