package vdx.mtg4s.mtgjson.raw

import org.scalatest.matchers.should.Matchers
import scala.io.Source
import vdx.mtg4s.mtgjson.MtgJsonSpec
import vdx.mtg4s.mtgjson.CirceSpec

class AllPrintingsSpec extends MtgJsonSpec with Matchers with CirceSpec {

  "AllPrintings bindings" should "have the correct structure" in {
    val json: String = Source.fromFile("AllPrintings.json").mkString

    decode[AllPrintings](json) shouldBe a[Right[_, Map[String, Set]]]
  }
}
