package vdx.mtg4s.mtgjson
package raw

import io.circe.Decoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LeadershipSkillsSpec extends AnyFlatSpec with Matchers with CirceSpec {
  "LeadershipSkills" should "have a circe decoder" in {
    decoderOf[LeadershipSkills] shouldBe a[Decoder[_]]
  }
}
