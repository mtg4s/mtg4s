package vdx.mtg4s.mtgjson

import java.util.UUID

import cats.instances.string._
import cats.kernel.Eq
import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

final case class MtgJsonId(uuid: UUID) extends AnyVal

object MtgJsonId {
  implicit val eq: Eq[MtgJsonId] = Eq.by(_.uuid.toString())
  implicit val codec: Codec[MtgJsonId] = deriveUnwrappedCodec

  def fromString(uuid: String): MtgJsonId =
    MtgJsonId(UUID.fromString(uuid))
}
