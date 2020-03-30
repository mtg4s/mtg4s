package vdx.mtg4s.mtgjson.raw

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class Token()

object Token {
  implicit val decoder: Decoder[Token] = deriveDecoder
}
