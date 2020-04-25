package vdx.mtg4s.mtgjson.raw

import enumeratum._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import vdx.mtg4s.mtgjson.raw.Legalities.Legality

case class Legalities(
  brawl: Option[Legality],
  commander: Option[Legality],
  duel: Option[Legality],
  future: Option[Legality],
  frontier: Option[Legality],
  legacy: Option[Legality],
  modern: Option[Legality],
  pauper: Option[Legality],
  penny: Option[Legality],
  pioneer: Option[Legality],
  standard: Option[Legality],
  vintage: Option[Legality]
)

object Legalities {
  implicit val decoder: Decoder[Legalities] = deriveDecoder

  sealed trait Legality extends EnumEntry

  object Legality extends Enum[Legality] with CirceEnum[Legality] {
    val values: IndexedSeq[Legality] = findValues

    case object Legal extends Legality
    case object Restricted extends Legality
    case object Banned extends Legality
  }
}
