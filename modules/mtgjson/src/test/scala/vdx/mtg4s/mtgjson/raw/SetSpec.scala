package vdx.mtg4s.mtgjson
package raw

import io.circe.Decoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SetSpec extends AnyFlatSpec with Matchers with CirceSpec {
  "Set" should "have a circe decoder" in {
    decoderOf[Set] shouldBe a[Decoder[_]]
  }
}
