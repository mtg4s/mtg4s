package vdx.mtg4s

import cats.effect.{ExitCode, IO, IOApp}
import cats.instances.string._
import cats.syntax.functor._
import vdx.mtg4s.terminal.LineReader.AutoCompletionSource
import vdx.mtg4s.terminal.{LineReader, Terminal}

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

      val autocomplete: AutoCompletionSource[String] = (str: String) => lines.filter(_.startsWith(str)).map(s => s -> s)

      lineReader
        .readLine("prompt > ", autocomplete)
        .as(ExitCode.Success)
    }
  }

}
