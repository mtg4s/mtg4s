package vdx.mtg4s.inventory.parser

import cats.data.NonEmptyList
import vdx.mtg4s.inventory.Inventory
import vdx.mtg4s.inventory.parser.Parser.ParserError

trait Parser[F[_]] {
  def parse(raw: String): F[Either[NonEmptyList[ParserError], Inventory]]
}

object Parser {
  sealed trait ParserError
  final case class ParsingError(message: String) extends ParserError
}
