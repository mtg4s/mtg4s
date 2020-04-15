package vdx.mtg4s.inventory.parser.deckbox

import cats.data.Chain
import cats.effect.Sync
import cats.instances.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import kantan.csv._
import kantan.csv.ops._
import monocle.Getter
import vdx.mtg4s.CardList.Card
import vdx.mtg4s._
import vdx.mtg4s.inventory._
import vdx.mtg4s.inventory.parser.Parser
import vdx.mtg4s.inventory.parser.Parser._

object DeckboxCsvParser {

  /**
   * Creates a Parser that can parse inventory files created on deckbox.org
   */
  def apply[F[_]: Sync, Repr, CardId](
    db: CardDB[F, Repr, SetName]
  )(implicit G: Getter[Repr, CardId]): Parser[F, CardId] = new Parser[F, CardId] {
    override def parse(raw: String): F[ParserResult[Inventory[CardId]]] =
      Sync[F]
        .delay(raw.trim.asCsvReader[RawDeckboxCard](rfc.withHeader(true)))
        .flatMap(
          _.foldLeft[Chain[F[ParserResult[Card[CardId]]]]](Chain.empty) { (chain, result) =>
            // +1 because of the header and +1 because line numbering in messages is not zero based
            implicit val pos: Pos = Pos(chain.length + 2)
            chain ++ Chain.one(
              result.fold(
                e =>
                  errorResultF[F, Card[CardId]](
                    ParsingError(s"Error parsing/decoding line ${pos.line}: ${e.toString}")
                  ),
                item => findOrParsingError(CardName(item.name), SetName(item.edition), item.count)
              )
            )
          }.traverse(identity)
            .map(_.traverse(identity).map(CardList(_)))
        )

    private[this] def findOrParsingError(name: CardName, set: SetName, count: Int)(
      implicit pos: Pos
    ): F[ParserResult[Card[CardId]]] =
      db.findByNameAndSet(name, set)
        .map(
          _.toRight(
            oneError(CardNotFoundError(s"Cannot find card in the database: ${name} (${set}) at line ${pos.line}"))
          ).map(r => Card(G.get(r), count))
        )
  }

  private[deckbox] final case class Pos(line: Long)

  /**
   * Representation of the single line of the deckbox csv
   */
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
