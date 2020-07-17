package vdx.mtg4s.mtgjson.raw

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

/**
 * The full representation of the AllPrintings.json database
 */
case class AllPrintings(data: Map[String, Set])

object AllPrintings {
  implicit val decoder: Decoder[AllPrintings] = deriveDecoder
}
