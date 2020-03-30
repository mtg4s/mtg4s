package vdx.mtg4s.mtgjson
package raw

import io.circe.Decoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardSpec extends AnyFlatSpec with Matchers with CirceSpec {
  "Card" should "have a circe decoder" in {
    decoderOf[Card] shouldBe a[Decoder[_]]
  }
}
