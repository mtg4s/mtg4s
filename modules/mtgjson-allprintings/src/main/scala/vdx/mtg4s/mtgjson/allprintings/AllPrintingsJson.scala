package vdx.mtg4s.mtgjson.allprintings

import cats.effect.Sync

import scala.io.Source

object AllPrintingsJson {
  def string[F[_]: Sync]: F[String] =
    Sync[F].delay(Source.fromResource("AllPrintings-4.6.3+20200518.json").mkString)
}
