package vdx.mtg4s.mtgjson

import cats.effect.ContextShift
import cats.effect.IO
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import vdx.mtg4s.mtgjson.MtgJson._
import vdx.mtg4s.mtgjson.allprintings.AllPrintingsJson
import vdx.mtg4s.mtgjson.raw.AllPrintings

import scala.concurrent.ExecutionContext

class MtgJsonSpec extends AnyFlatSpec with Matchers with EitherValues {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "MtgJson[F, Repr]" should "return a ParsingFailure when the json is invalid" in runIO {
    val invalidJson = getResource("invalid.json")

    MtgJson[IO, AllPrintings](invalidJson).db
      .map(_.left.value shouldBe a[ParsingFailure])
  }

  it should "return a DecodingFailure when the given representation doesn't match the file" in runIO {
    val invalidMtgJson = getResource("valid.json")

    MtgJson[IO, AllPrintings](invalidMtgJson).db
      .map(_.left.value shouldBe a[DecodingFailure])
  }

  it should "load the database given the raw representation" in runIO {
    MtgJson[IO, AllPrintings](AllPrintingsJson.string[IO]).db
      .map(_ shouldBe a[Right[_, AllPrintings]])
  }

  def runIO[A](test: IO[A]): A =
    test.unsafeRunSync()
}
