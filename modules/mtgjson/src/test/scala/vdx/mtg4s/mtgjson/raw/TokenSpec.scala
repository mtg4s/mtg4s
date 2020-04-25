package vdx.mtg4s.mtgjson
package raw

import io.circe.Decoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TokenSpec extends AnyFlatSpec with Matchers with CirceSpec {
  "Token" should "have a circe decoder" in {
    decoderOf[Token] shouldBe a[Decoder[_]]
  }
}
