package vdx.mtg4s

import scala.io.Source

import cats.effect.IO

package object mtgjson {
  def getResource(name: String): IO[String] =
    IO.delay(Source.fromResource(name).mkString)
}
