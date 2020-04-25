package vdx.mtg4s.mtgjson.raw

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class PurchaseUrls(
  cardmarket: Option[String], // This is not optional in the docs
  tcgplayer: Option[String], // This is not optional in the docs
  mtgstocks: Option[String] // This is not optional in the docs
)

object PurchaseUrls {
  implicit val decoder: Decoder[PurchaseUrls] = deriveDecoder
}
