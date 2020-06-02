package vdx.mtg4s.mtgjson.raw

import scala.concurrent.ExecutionContext

import cats.effect.{ContextShift, IO}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import vdx.mtg4s.mtgjson.CirceSpec
import vdx.mtg4s.mtgjson.allprintings.AllPrintingsJson

class AllPrintingsSpec extends AnyFlatSpec with Matchers with CirceSpec {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "AllPrintings bindings" should "have the correct structure" in {
    AllPrintingsJson
      .string[IO]
      .map(decode[AllPrintings](_) shouldBe a[Right[_, Map[String, Set]]])
      .unsafeRunSync()
  }
}
