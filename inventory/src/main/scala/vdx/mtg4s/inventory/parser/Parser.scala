package vdx.mtg4s.inventory.parser

import cats.Applicative
import cats.data.NonEmptyList
import cats.kernel.Eq
import vdx.mtg4s.inventory.Inventory
import vdx.mtg4s.inventory.parser.Parser.ParserResult

/**
 * An interface to parse inventory files from different sources (deckbox, etc)
 */
trait Parser[F[_], CardId] {

  /**
   * Parse the given string and return a validated inventory.
   *
   * Cards that cannot be mapped to an {{MtgJsonId}} will be treated as invalid lines and the result will be a
   * Left value with the list of errors
   */
  def parse(raw: String): F[ParserResult[Inventory[CardId]]]
}

object Parser {
  type ParserResult[A] = Either[NonEmptyList[ParserError], A]

  sealed trait ParserError
  final case class ParsingError(message: String) extends ParserError
  final case class CardNotFoundError(message: String) extends ParserError

  object ParserError {
    implicit val eq: Eq[ParserError] = Eq.fromUniversalEquals
  }

  private[parser] def oneError[A](error: ParserError): NonEmptyList[ParserError] =
    NonEmptyList.one(error)

  private[parser] def errorResult[A](error: ParserError): ParserResult[A] =
    Left(oneError(error))

  private[parser] def errorResultF[F[_]: Applicative, A](error: ParserError): F[ParserResult[A]] =
    Applicative[F].pure(errorResult(error))

}
