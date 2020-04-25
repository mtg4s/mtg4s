package vdx.mtg4s.mtgjson.raw

import enumeratum.EnumEntry.Snakecase
import enumeratum._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import vdx.mtg4s.mtgjson.raw.Set.SetType

import java.time.LocalDate

case class Set(
  baseSetSize: Int,
  block: Option[String],
  // boosterV3: List[String], // ListStringOrString,
  cards: List[Card],
  code: String,
  codeV3: Option[String],
  isForeignOnly: Option[Boolean],
  isFoilOnly: Option[Boolean],
  isOnlineOnly: Option[Boolean],
  isPartialPreview: Option[Boolean],
  keyruneCode: String,
  mcmName: Option[String], // This is not optional in the docs
  mcmId: Option[Integer], // This is not optional in the docs
  meta: Map[String, String],
  mtgoCode: Option[String], // This is not optional in the docs
  name: String,
  parentCode: Option[String], // This is not optional in the docs
  releaseDate: LocalDate,
  tcgplayerGroupId: Option[Int], // This is not optional in the docs
  tokens: List[Token],
  totalSetSize: Int,
  translations: Map[String, String],
  `type`: SetType
)

object Set {

  implicit val decoder: Decoder[Set] = deriveDecoder

  sealed trait SetType extends EnumEntry with Snakecase

  object SetType extends Enum[SetType] with CirceEnum[SetType] {
    val values: IndexedSeq[SetType] = findValues

    case object Archenemy extends SetType
    case object Box extends SetType
    case object Commander extends SetType
    case object Core extends SetType
    case object DraftInnovation extends SetType
    case object DuelDeck extends SetType
    case object Expansion extends SetType
    case object FromTheVault extends SetType
    case object Funny extends SetType
    case object Masterpiece extends SetType
    case object Masters extends SetType
    case object Memorabilia extends SetType
    case object Planechase extends SetType
    case object PremiumDeck extends SetType
    case object Promo extends SetType
    case object Spellbook extends SetType
    case object Starter extends SetType
    case object Token extends SetType
    case object TreasureChest extends SetType
    case object Vanguard extends SetType
  }
}
