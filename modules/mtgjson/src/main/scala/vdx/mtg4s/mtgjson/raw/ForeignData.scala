package vdx.mtg4s.mtgjson.raw

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class ForeignData(
  flavorText: Option[String], // Not optional in the docs
  language: String, // TODO: enum?
  multiverseId: Option[Int], // This is not optional in the docs
  name: String,
  text: Option[String], //  This is not optional in the docs
  `type`: Option[String] // Not optional in the docs
)

object ForeignData {
  implicit val decoder: Decoder[ForeignData] = deriveDecoder
}
