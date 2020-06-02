package vdx.mtg4s

import cats.effect.{ExitCode, IO, IOApp}
import vdx.mtg4s.terminal.{Console, LineReader, Terminal}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    Terminal[IO].use { terminal =>
      val lineReader = LineReader[IO](terminal)
      val console = Console[IO](terminal, lineReader)

      // val lines = List(
      //   "foo",
      //   "foobar",
      //   "foobarbaz",
      //   "bar",
      //   "baz"
      // )

      // val autocomplete: Autocomplete[IO] =
      //   (str: String) => IO.delay(lines.filter(_.startsWith(str)))

      for {
        _   <- console.clearScreen()
        str <- console.readLine("prompt > ")
        _   <- console.putStrLn()
        _   <- console.putStrLn(s"The input was: $str")

      } yield ExitCode.Success
    }
  }

}
