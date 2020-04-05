package vdx.mtg4s

import cats.data.Chain

package object inventory {
  type Inventory = Chain[InventoryItem]
}
