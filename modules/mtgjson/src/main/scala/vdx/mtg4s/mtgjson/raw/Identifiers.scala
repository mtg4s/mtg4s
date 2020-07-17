package vdx.mtg4s.mtgjson.raw

import java.util.UUID

import cats.kernel.Eq
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class Identifiers(
  mcmId: Option[Integer], // Not optional in the docs
  mcmMetaId: Option[Integer], // Not optional in the docs
  mtgArenaId: Option[Int],
  mtgoFoilId: Option[Int],
  mtgoId: Option[Int],
  mtgstocksId: Option[Int], // This is not optional in the docs
  multiverseId: Option[Int], // This is not optional in the docs
  scryfallId: UUID,
  scryfallOracleId: UUID,
  scryfallIllustrationId: Option[UUID],
  tcgplayerProductId: Option[Integer] // This is not optional in the docs
)

object Identifiers {
  implicit val eq: Eq[Identifiers] = Eq.fromUniversalEquals
  implicit val decoder: Decoder[Identifiers] = deriveDecoder
}
