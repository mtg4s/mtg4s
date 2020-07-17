package vdx.mtg4s.mtgjson.allprintings

import java.security.MessageDigest

import cats.effect.IO
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AllPrintingsJsonSpec extends AnyWordSpec with Matchers {

  "string" should {
    "read the file without errors" in {
      AllPrintingsJson
        .string[IO]
        .map { str =>
          MessageDigest
            .getInstance("SHA-256")
            .digest(str.getBytes)
            .map("%02x".format(_))
            .mkString should be(
            "7b65ee9c9dd524ebaf48266c153a5e2d6e189396cbfbb73c6f0241ef2f120d10"
          )
        }
        .unsafeRunSync()
    }
  }
}
