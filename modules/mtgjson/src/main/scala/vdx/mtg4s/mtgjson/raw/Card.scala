package vdx.mtg4s.mtgjson.raw

import java.util.UUID

import cats.kernel.Eq
import enumeratum.EnumEntry.{Lowercase, Snakecase}
import enumeratum._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import vdx.mtg4s.mtgjson.MtgJsonId
import vdx.mtg4s.mtgjson.raw.Card._

case class Card(
  artist: Option[String], // This is not optional in the docs
  borderColor: BorderColor,
  colorIdentity: List[Color],
  colorIndicater: Option[List[Color]],
  colors: List[Color],
  convertedManaCost: Double,
  count: Option[Int],
  edhrecRank: Option[Integer],
  faceConvertedManaCost: Option[Double], // This is not optional in the docs
  flavorText: Option[String],
  foreignData: List[ForeignData],
// @deprecated("frame effect is deprecated", )
  frameEffect: Option[FrameEffect], // This is not optional in the docs
  frameEffects: Option[List[FrameEffect]], //  This is not optional in the docs
  frameVersion: FrameVersion,
  hand: Option[String],
  hasFoil: Option[Boolean],
// @deprecated()
  hasAlternativeDeckLimit: Option[Boolean],
  hasNonFoil: Option[Boolean],
  identifiers: Identifiers,
  isAlternative: Option[Boolean],
  isArena: Option[Boolean],
  isFullArt: Option[Boolean],
  isMtgo: Option[Boolean],
  isOnlineOnly: Option[Boolean],
  isOversized: Option[Boolean],
  isPaper: Option[Boolean],
  isPromo: Option[Boolean],
  isReprint: Option[Boolean],
  isReserved: Option[Boolean],
  isStarter: Option[Boolean],
  isStorySpotlight: Option[Boolean],
  isTextless: Option[Boolean],
  isTimeshifetd: Option[Boolean],
  layout: Layout,
  leadershipSkills: Option[LeadershipSkills],
  legalities: Legalities,
  life: Option[String],
  loyalty: Option[String],
  manaCost: Option[String], // This is not optional in the docs
  name: String,
  number: String,
  originalText: Option[String], // This is not optional in the docs
  originalType: Option[String], //  This is not optional in the docs
  otherFaceIds: Option[List[UUID]],
  power: Option[String], // This is not optional in the docs
  prices: Option[Prices], // This is not optional in the docs
  printings: List[String],
//  promoTypes: PromoTypes,
  purchaseUrls: Option[PurchaseUrls], // This is not optional in the docs
  rarity: Rarity,
  reverseRelated: Option[List[String]],
  rulings: List[Ruling],
  side: Option[Side],
  subtypes: List[String],
  supertypes: List[String],
  text: Option[String], //  This is not optional in the docs
  toughness: Option[String], //  This is not optional in the docs
  `type`: String,
  types: List[String],
  uuid: MtgJsonId, // v5
  variations: Option[List[UUID]], // This is not optional in the docs
  watermark: Option[String]
)

object Card {
  implicit val decoder: Decoder[Card] = deriveDecoder
  implicit val eq: Eq[Card] = Eq.fromUniversalEquals

  sealed trait BorderColor extends EnumEntry with Lowercase

  object BorderColor extends Enum[BorderColor] with CirceEnum[BorderColor] {
    val values: IndexedSeq[BorderColor] = findValues

    case object Black extends BorderColor
    case object Borderless extends BorderColor
    case object Gold extends BorderColor
    case object Silver extends BorderColor
    case object White extends BorderColor
  }

  sealed class Color(override val entryName: String) extends EnumEntry

  object Color extends Enum[Color] with CirceEnum[Color] {
    val values: IndexedSeq[Color] = findValues

    case object Black extends Color("B")
    case object Blue extends Color("U")
    case object Green extends Color("G")
    case object Red extends Color("R")
    case object White extends Color("W")
  }

  sealed trait FrameEffect extends EnumEntry with Lowercase

  object FrameEffect extends Enum[FrameEffect] with CirceEnum[FrameEffect] {
    val values: IndexedSeq[FrameEffect] = findValues

    case object Colorshifted extends FrameEffect
    case object Compasslanddfc extends FrameEffect
    case object Devoid extends FrameEffect
    case object Draft extends FrameEffect
    case object Legendary extends FrameEffect
    case object Miracle extends FrameEffect
    case object Mooneldrazidfc extends FrameEffect
    case object Nyxtouched extends FrameEffect
    case object Nyxborn extends FrameEffect // This was missing from the docs
    case object Originpwdfc extends FrameEffect
    case object Sunmoondfc extends FrameEffect
    case object Tombstone extends FrameEffect
    case object Extendedart extends FrameEffect // This was missing from the docs
    case object Showcase extends FrameEffect // This was missing from the docs
    case object Inverted extends FrameEffect // This was missing from the docs
    case object Waxingandwaningmoondfc extends FrameEffect // This was missing from the docs
    case object Companion extends FrameEffect
  }

  sealed class FrameVersion(override val entryName: String) extends EnumEntry

  object FrameVersion extends Enum[FrameVersion] with CirceEnum[FrameVersion] {
    val values: IndexedSeq[FrameVersion] = findValues

    case object FrameVersion1993 extends FrameVersion("1993")
    case object FrameVersion1997 extends FrameVersion("1997")
    case object FrameVersion2003 extends FrameVersion("2003")
    case object FrameVersion2015 extends FrameVersion("2015")
    case object FrameVersionFuture extends FrameVersion("future")
  }

  sealed trait Layout extends EnumEntry with Snakecase

  object Layout extends Enum[Layout] with CirceEnum[Layout] {
    val values: IndexedSeq[Layout] = findValues

    case object Normal extends Layout
    case object Split extends Layout
    case object Flip extends Layout
    case object Transform extends Layout
    case object Meld extends Layout
    case object Leveler extends Layout
    case object Saga extends Layout
    case object Planar extends Layout
    case object Scheme extends Layout
    case object Vanguard extends Layout
    case object Token extends Layout
    case object DoubleFacedToken extends Layout
    case object Emblem extends Layout
    case object Augment extends Layout
    case object Aftermath extends Layout
    case object Host extends Layout
    case object Adventure extends Layout // This was missing from the docs
  }

  sealed trait Rarity extends EnumEntry with Lowercase

  object Rarity extends Enum[Rarity] with CirceEnum[Rarity] {
    val values: IndexedSeq[Rarity] = findValues

    case object Basic extends Rarity
    case object Common extends Rarity
    case object Uncommon extends Rarity
    case object Rare extends Rarity
    case object Mythic extends Rarity
  }

  sealed trait Side extends EnumEntry with Lowercase

  object Side extends Enum[Side] with CirceEnum[Side] {
    val values: IndexedSeq[Side] = findValues

    case object A extends Side
    case object B extends Side
    case object C extends Side
    case object D extends Side
    case object E extends Side
  }
}
