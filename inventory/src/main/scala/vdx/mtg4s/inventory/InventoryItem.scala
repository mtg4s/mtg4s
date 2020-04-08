package vdx.mtg4s.inventory

import cats.kernel.Eq
import vdx.mtg4s.MtgJsonId

case class InventoryItem(id: MtgJsonId, count: Int)

object InventoryItem {
  implicit val eq: Eq[InventoryItem] = Eq.fromUniversalEquals
}
