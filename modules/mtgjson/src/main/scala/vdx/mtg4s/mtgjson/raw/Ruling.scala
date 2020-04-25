package vdx.mtg4s.mtgjson.raw

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import java.time.LocalDate

case class Ruling(
  date: LocalDate,
  text: String
)

object Ruling {
  implicit val decoder: Decoder[Ruling] = deriveDecoder
}
