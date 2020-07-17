package vdx.mtg4s.mtgjson

import java.io.{File, FileOutputStream, InputStream, OutputStream}
import java.net.URL

import scala.concurrent.ExecutionContext.global

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import cats.syntax.flatMap._
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import vdx.fetchfile._
import vdx.fetchfile.http4s.Http4sClient

object UpdateMirror extends IOApp {

  val mtgjsonUrl = new URL("https://mtgjson.com/api/v5/AllPrintings.json.gz")
  val mtgjsonMetaUrl = uri"https://mtgjson.com/api/v5/Meta.json"

  case class Meta(version: String)
  case class MtgJsonMeta(meta: Meta)

  def run(args: List[String]): IO[ExitCode] = {
    implicit val clock: MonotonicClock = MonotonicClock.system
    (for {
      client   <- BlazeClientBuilder[IO](global).resource
      blocker  <- Blocker[IO]
      tempFile <- createTempFile("mtjgson", ".json")
    } yield (client, blocker, tempFile)).use {
      case (client, blocker, mtgjsonFile) =>
        implicit val backend: HttpClient[IO] = Http4sClient(client)

        val downloader = Downloader[IO](blocker, Progress.consoleProgress[IO])
        val mirror = FileMirror[IO]("AllPrintings", "json.gz")

        for {
          meta <- client.expect[MtgJsonMeta](mtgjsonMetaUrl)(jsonOf[IO, MtgJsonMeta])
          _    <- IO.delay(println(s"Current version is: ${meta.meta.version}"))
          _ <- mirror
            .downloadFile(meta.meta.version)
            .use(updateIfNeeded(meta.meta.version, mtgjsonFile, downloader, mirror))
        } yield ExitCode.Success
    }
  }

  private def updateIfNeeded(version: String, mtgjsonFile: File, downloader: Downloader[IO], mirror: FileMirror[IO])(
    maybeFile: Option[InputStream]
  ) =
    maybeFile.fold(fetchAndUpload(version, mtgjsonFile, downloader, mirror))(_ =>
      IO.delay(println("The latest version already mirrored"))
    )

  private def fetchAndUpload(
    version: String,
    mtgjsonFile: File,
    downloader: Downloader[IO],
    mirror: FileMirror[IO]
  ): IO[Unit] =
    IO.delay(println(mtgjsonFile.getPath())) >>
      downloader.fetch(mtgjsonUrl, toOutputResource(mtgjsonFile)) >>
      mirror.uploadFile(mtgjsonFile, version)

  private def toOutputResource(file: File): Resource[IO, OutputStream] =
    Resource.fromAutoCloseable(IO.delay(new FileOutputStream(file)))

  private def createTempFile(prefix: String, ext: String): Resource[IO, File] =
    Resource.make(IO.delay(File.createTempFile(prefix, ext)))(f => IO.delay(f.delete()).void)
}
