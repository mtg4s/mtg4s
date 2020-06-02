package vdx.mtg4s.mtgjson.allprintings

import scala.io.Source

import cats.effect.Sync

object AllPrintingsJson {
  def string[F[_]: Sync]: F[String] =
    Sync[F].delay(Source.fromResource("AllPrintings-4.6.3+20200518.json").mkString)
}
