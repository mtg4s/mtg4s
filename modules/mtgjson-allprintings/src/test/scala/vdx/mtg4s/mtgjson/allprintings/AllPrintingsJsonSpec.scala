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
            "604a86372ca9cb3800613f7df7b111c66f1e2fcc6bb60a6a3fb2177dadb0b0d1"
          )
        }
        .unsafeRunSync()
    }
  }
}
