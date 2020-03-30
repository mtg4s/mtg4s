package vdx.mtg4s.mtgjson

import cats.MonadError
import cats.effect.Sync
import cats.syntax.applicativeError._
import io.circe.{
  Decoder,
  Error => CirceError,
  DecodingFailure => CirceDecodingFailure,
  ParsingFailure => CirceParsingFailure
}
import io.circe.parser.decode
import vdx.mtg4s.mtgjson.MtgJson.Error

import java.io.File

import scala.io.Source
import java.io.FileNotFoundException

trait MtgJson[F[_], Repr] {
  def load(): F[Either[Error, Repr]]
}

object MtgJson {

  sealed trait Error
  final case class ParsingFailure(message: String) extends Error
  final case class DecodingFailure(message: String) extends Error
  final case class FileNotFound(message: String) extends Error

  def apply[F[_]: Sync: MonadError[*[_], Throwable], Repr: Decoder](mtgjson: File): MtgJson[F, Repr] =
    new MtgJson[F, Repr] {
      override def load(): F[Either[Error, Repr]] =
        Sync[F].delay(decode[Repr](Source.fromFile(mtgjson).mkString).left.map(circeErrorToMtgJsonError)).recover {
          case e: FileNotFoundException => Left(FileNotFound(e.getMessage()))
        }

      private def circeErrorToMtgJsonError(error: CirceError): Error = error match {
        case e: CirceParsingFailure  => ParsingFailure(e.getMessage())
        case e: CirceDecodingFailure => DecodingFailure(e.getMessage())
      }
    }
}
