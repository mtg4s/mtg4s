package vdx.mtg4s.mtgjson
package raw

import io.circe.Decoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RulingSpec extends AnyFlatSpec with Matchers with CirceSpec {
  "PurchaseUrls" should "have a circe decoder" in {
    decoderOf[Ruling] shouldBe a[Decoder[_]]
  }
}
