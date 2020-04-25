package vdx.mtg4s

import org.scalacheck.Gen
import vdx.mtg4s.SetName

object Generators {
  val validCards: Gen[(CardName, List[SetName])] = Gen.oneOf(
    CardName("Primeval Titan") -> List(SetName("Iconic Masters")),
    CardName("Snapcaster Mage") -> List(SetName("Innistrad")),
    CardName("Lightning Bolt") -> List(SetName("Masters 25")),
    CardName("Karn, the Great Creator") -> List(SetName("War of the Spark"))
  )
}
