package vdx.mtg4s.mtgjson

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

import java.util.UUID

final case class MtgJsonId(uuid: UUID) extends AnyVal

object MtgJsonId {
  implicit val codec: Codec[MtgJsonId] = deriveUnwrappedCodec
}
