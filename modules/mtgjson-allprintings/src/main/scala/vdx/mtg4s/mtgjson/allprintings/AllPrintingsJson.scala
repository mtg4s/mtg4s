package vdx.mtg4s.mtgjson.allprintings

import scala.io.Source

import cats.effect.Sync

object AllPrintingsJson {
  val version: String = "5.0.0+20200714"

  def string[F[_]: Sync]: F[String] =
    Sync[F].delay(Source.fromResource(s"AllPrintings-$version.json").mkString)
}
