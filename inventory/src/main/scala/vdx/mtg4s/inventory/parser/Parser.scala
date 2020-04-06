package vdx.mtg4s.inventory.parser

import cats.Applicative
import cats.data.NonEmptyList
import vdx.mtg4s.inventory.Inventory
import vdx.mtg4s.inventory.parser.Parser.ParserResult

trait Parser[F[_]] {
  def parse(raw: String): F[ParserResult[Inventory]]
}

object Parser {
  type ParserResult[A] = Either[NonEmptyList[ParserError], A]

  sealed trait ParserError
  final case class ParsingError(message: String) extends ParserError

  private[parser] def oneError[A](error: ParserError): NonEmptyList[ParserError] =
    NonEmptyList.one(error)

  private[parser] def errorResult[A](error: ParserError): ParserResult[A] =
    Left(oneError(error))

  private[parser] def errorResultF[F[_]: Applicative, A](error: ParserError): F[ParserResult[A]] =
    Applicative[F].pure(errorResult(error))

}
