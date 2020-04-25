package vdx.mtg4s.mtgjson.raw

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class Prices(
  paper: Option[Map[String, Double]], // This is not optional in the docs
  paperFoil: Option[Map[String, Double]], // This is not optional in the docs
  mtgo: Option[Map[String, Double]], // This is not optional in the docs
  mtgoFoil: Option[Map[String, Double]] // This is not optional in the docs
)

object Prices {
  implicit val decoder: Decoder[Prices] = deriveDecoder[Prices]
}
