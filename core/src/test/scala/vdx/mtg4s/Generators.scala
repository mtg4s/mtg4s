package vdx.mtg4s

import org.scalacheck.Gen

object Generators {
  val validCards: Gen[(CardName, List[Set])] = Gen.oneOf(
    CardName("Primeval Titan") -> List(Set("Iconic Masters")),
    CardName("Snapcaster Mage") -> List(Set("Innistrad")),
    CardName("Lightning Bolt") -> List(Set("Masters 25")),
    CardName("Karn, the Great Creator") -> List(Set("War of the Spark"))
  )
}
