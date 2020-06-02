package vdx.mtg4s.mtgjson

import java.util.UUID

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

final case class MtgJsonId(uuid: UUID) extends AnyVal

object MtgJsonId {
  implicit val codec: Codec[MtgJsonId] = deriveUnwrappedCodec
}
