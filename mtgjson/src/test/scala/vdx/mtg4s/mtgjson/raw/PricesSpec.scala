package vdx.mtg4s.mtgjson
package raw

import io.circe.Decoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PricesSpec extends AnyFlatSpec with Matchers with CirceSpec {
  "Prices" should "have a circe decoder" in {
    decoderOf[Prices] shouldBe a[Decoder[_]]
  }
}
