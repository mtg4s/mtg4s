package vdx.mtg4s

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import cats.syntax.functor._
import vdx.mtg4s.terminal.LineReader
import vdx.mtg4s.terminal.Terminal

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    Terminal[IO].use { terminal =>
      val lineReader = LineReader[IO](terminal)

      val lines = List(
        "foo",
        "foobar",
        "foobarbaz",
        "bar",
        "baz"
      )

      lineReader
        .readLine("prompt > ", Option((str: String) => lines.filter(_.startsWith(str))))
        .as(ExitCode.Success)
    }
  }

}
