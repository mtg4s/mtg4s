package vdx.mtg4s.mtgjson.raw

import org.scalatest.matchers.should.Matchers
import vdx.mtg4s.mtgjson.CirceSpec
import vdx.mtg4s.mtgjson.MtgJsonTestDB

import scala.io.Source

class AllPrintingsSpec extends MtgJsonTestDB with Matchers with CirceSpec {

  "AllPrintings bindings" should "have the correct structure" in {
    val json: String = Source.fromFile("AllPrintings.json").mkString

    decode[AllPrintings](json) shouldBe a[Right[_, Map[String, Set]]]
  }
}
