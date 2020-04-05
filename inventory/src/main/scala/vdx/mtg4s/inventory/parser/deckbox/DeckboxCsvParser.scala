package vdx.mtg4s.inventory.parser.deckbox

import cats.data.Chain
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.instances.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroup._
import cats.syntax.traverse._
import kantan.csv._
import kantan.csv.ops._
import monocle.Getter
import vdx.mtg4s._
import vdx.mtg4s.inventory._
import vdx.mtg4s.inventory.parser.Parser
import vdx.mtg4s.inventory.parser.Parser._

object DeckboxCsvParser {
  def apply[F[_]: Sync, Repr](db: CardDB[F, Repr])(implicit G: Getter[Repr, MtgJsonId]): Parser[F] =
    raw =>
      Sync[F]
        .delay(raw.trim.asCsvReader[RawDeckboxCard](rfc.withHeader(true)))
        .map(s => s.foldLeft[Chain[ReadResult[RawDeckboxCard]]](Chain.empty)(_ |+| Chain.one(_)))
        .flatMap(
          _.map(
            _.fold[F[Either[NonEmptyList[ParserError], InventoryItem]]](
              _ => Sync[F].pure(Left(NonEmptyList.one(ParsingError("Error parsing line")))),
              item =>
                db.find(CardName(item.name), Set("foobar"))
                  .map(
                    _.fold[Either[NonEmptyList[ParserError], InventoryItem]](
                      Left(NonEmptyList.one(ParsingError("Cannot find card in db")))
                    )(r => Right(reprToInventoryItem(item.count)(r)))
                  )
            )
          ).traverse(identity)
            .map(
              _.traverse(identity)
            )
        )

  private[this] def reprToInventoryItem[Repr](count: Int)(r: Repr)(implicit G: Getter[Repr, MtgJsonId]): InventoryItem =
    InventoryItem(G.get(r), count)

  private[deckbox] final case class RawDeckboxCard(
    count: Int,
    name: String,
    edition: String,
    cardNumber: Int,
    condition: String,
    language: String,
    foil: Option[String]
  )

  private val ColumnCount = 0
  private val ColumnName = 2
  private val ColumnEdition = 3
  private val ColumnCardNumber = 4
  private val ColumnCondition = 5
  private val ColumnLanguage = 6
  private val ColumnFoil = 7

  private[deckbox] implicit val cardDecoder: RowDecoder[RawDeckboxCard] =
    RowDecoder.decoder(
      ColumnCount,
      ColumnName,
      ColumnEdition,
      ColumnCardNumber,
      ColumnCondition,
      ColumnLanguage,
      ColumnFoil
    )(RawDeckboxCard.apply)

}
