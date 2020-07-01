package vdx.mtg4s.mtgjson.raw

import cats.kernel.Eq
import cats.syntax.eq._

case class CardAndSet(
  card: Card,
  set: Set
)

object CardAndSet {
  implicit val eq: Eq[CardAndSet] = Eq.instance((x, y) => x.card === y.card && x.set === y.set)
}
