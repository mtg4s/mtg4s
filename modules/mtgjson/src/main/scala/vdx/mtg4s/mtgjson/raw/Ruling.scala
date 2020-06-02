package vdx.mtg4s.mtgjson.raw

import java.time.LocalDate

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class Ruling(
  date: LocalDate,
  text: String
)

object Ruling {
  implicit val decoder: Decoder[Ruling] = deriveDecoder
}
