package vdx.mtg4s.mtgjson

import cats.effect.IO
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import vdx.mtg4s.mtgjson.MtgJson._
import vdx.mtg4s.mtgjson.raw.AllPrintings

import java.io.File

class MtgJsonSpec extends MtgJsonTestDB with Matchers with EitherValues {
  "MtgJson[F, Repr]" should "return FileNotfound when the file doesn't exist" in {
    val nonExistentFile = new File("/nonexistent.example")

    val result = MtgJson[IO, AllPrintings](nonExistentFile).load().unsafeRunSync
    result.left.value shouldBe a[FileNotFound]
  }

  it should "return a ParsingFailure when the json is invalid" in {
    val invalidFile = new File(getResource("invalid.json").toURI())

    val result = MtgJson[IO, AllPrintings](invalidFile).load().unsafeRunSync()
    result.left.value shouldBe a[ParsingFailure]

  }

  it should "return a DecodingFailure when the given representation doesn't match the file" in {
    val invalidFile = new File(getResource("valid.json").toURI())

    val result = MtgJson[IO, List[String]](invalidFile).load().unsafeRunSync()
    result.left.value shouldBe a[DecodingFailure]
  }

  it should "load the database given the raw representation" in {
    val result = MtgJson[IO, AllPrintings](allPringtingsJson).load().unsafeRunSync()
    result shouldBe a[Right[_, AllPrintings]]
  }
}
