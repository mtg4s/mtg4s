package vdx.mtg4s.mtgjson.raw

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class LeadershipSkills(
  brawl: Boolean,
  commander: Boolean,
  oathbreaker: Boolean
)

object LeadershipSkills {
  implicit val decoder: Decoder[LeadershipSkills] = deriveDecoder
}
