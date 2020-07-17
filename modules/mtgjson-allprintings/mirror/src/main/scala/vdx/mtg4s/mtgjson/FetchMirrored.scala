package vdx.mtg4s.mtgjson

import java.io.{File, FileOutputStream}

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import fs2._
import fs2.io._
import vdx.mtg4s.mtgjson.allprintings.AllPrintingsJson

object FetchMirrored extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val mirror = FileMirror[IO]("AllPrintings", "json.gz")
    val bufferSize = 64 * 1024

    (for {
      blocker       <- Blocker[IO]
      maybeInStream <- mirror.downloadFile(AllPrintingsJson.version)
    } yield (blocker, maybeInStream)).use {
      case (blocker, maybeInStream) =>
        maybeInStream.fold(IO.pure(ExitCode.Error)) { inStream =>
          readInputStream(IO.pure(inStream), bufferSize, blocker)
            .through(compression.gunzip(bufferSize))
            .flatMap(_.content)
            .through(writeOutputStream[IO](IO.delay(new FileOutputStream(destFile(AllPrintingsJson.version))), blocker))
            .compile
            .drain
            .as(ExitCode.Success)
        }
    }
  }

  private def destFile(version: String): File =
    new File(s"modules/mtgjson-allprintings/src/main/resources/AllPrintings-$version.json")

}
