package vdx.mtg4s

import cats.effect.IO

import scala.io.Source

package object mtgjson {
  def getResource(name: String): IO[String] =
    IO.delay(Source.fromResource(name).mkString)
}
