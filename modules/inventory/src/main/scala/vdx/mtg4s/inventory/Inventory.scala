package vdx.mtg4s.inventory

import cats.data.Chain
import vdx.mtg4s.inventory.Inventory.PaperCard

case class Inventory[CardId](cards: Chain[PaperCard[CardId]])

object Inventory {
  case class PaperCard[CardId](id: CardId, count: Int, foil: Boolean)
}
