package vdx.mtg4s

import cats.effect.{ExitCode, IO, IOApp}
import cats.instances.string._
import vdx.mtg4s.terminal._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    Terminal[IO].use { terminal =>
      val lineReader = LineReader[IO](terminal)
      val console = Console[IO](terminal, lineReader)

      val autocomplete: AutoCompletionSource[String] = str =>
        List(
          "foo",
          "bar",
          "baz",
          "foobar",
          "foobarbaz"
        ).filter(_.startsWith(str)).map(s => s -> s)

      implicit val acConfig: AutoCompletionConfig[String] = AutoCompletionConfig.defaultAutoCompletionConfig.copy(
        onResultChange = (maybeString, write) => write(
          TerminalControl.savePos() +
            TerminalControl.down(1) +
            TerminalControl.back(999) +
            TerminalControl.clearLine() +
            s"Currently selected: $maybeString" +
            TerminalControl.restorePos()
        )
      )

      for {
        _ <- console.clearScreen()
        _ <- console.moveToLastLine()
        _ <- console.putStrLn(TerminalControl.up())
        _ <- console.readLine("prompt > ", autocomplete)
      } yield ExitCode.Success
    }
  }

}
