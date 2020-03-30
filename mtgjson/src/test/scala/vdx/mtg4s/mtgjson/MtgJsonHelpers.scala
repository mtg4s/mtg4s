package vdx.mtg4s.mtgjson

import cats.syntax.functor._
import cats.instances.option._
import cats.effect.{Blocker, IO, Resource}
import vdx.fetchfile.{Downloader, HttpURLConnectionBackend, Progress}

import scala.concurrent.ExecutionContext

import java.io.{File, FileOutputStream}
import java.net.URL
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.BeforeAndAfterAll
import vdx.fetchfile.MonotonicClock
import fs2.Pipe

abstract class MtgJsonSpec extends AnyFlatSpec with BeforeAndAfterAll {
  override def beforeAll(): Unit = ensureMtgJsonFileExists()

  def ensureMtgJsonFileExists(): Unit = {
    val outFile = new File("AllPrintings.json")

    if (!outFile.exists()) {
      println("Downloading mtgjson db, this may take a while...")

      Blocker[IO].use { blocker =>
        implicit val cs = IO.contextShift(ExecutionContext.global)
        implicit val client = HttpURLConnectionBackend[IO](blocker, 1024 * 16)
        implicit val clock: MonotonicClock = MonotonicClock.system()

        val progress: Int => Pipe[IO, Byte, Unit] =
          sys.env.get("CI").as(Progress.noop[IO]).getOrElse(Progress.consoleProgress)

        Downloader[IO](blocker, progress)
          .fetch(
            new URL("https://www.mtgjson.com/files/AllPrintings.json"),
            Resource.fromAutoCloseable(IO.delay(new FileOutputStream(outFile)))
          )
      }.unsafeRunSync()
    }
  }

}
