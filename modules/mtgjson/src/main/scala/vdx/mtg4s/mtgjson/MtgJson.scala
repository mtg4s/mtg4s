package vdx.mtg4s.mtgjson

import cats.effect.Sync
import cats.syntax.functor._
import io.circe.parser.decode
import io.circe.{
  Decoder,
  DecodingFailure => CirceDecodingFailure,
  Error => CirceError,
  ParsingFailure => CirceParsingFailure
}
import vdx.mtg4s.mtgjson.MtgJson.Error

/**
 * An interface to safely acquire an in memory instance of the MTGJson database.
 *
 * The representation is configurable through the Repr type parameter, so that it can
 * be optimised to the task (e.g. use a subset of the fields to save memory, etc)
 */
trait MtgJson[F[_], Repr] {

  /**
   * Returns either an error or the given representation of the database
   */
  def db: F[Either[Error, Repr]]
}

object MtgJson {

  sealed trait Error
  final case class ParsingFailure(message: String) extends Error
  final case class DecodingFailure(message: String) extends Error

  /**
   * Creates an instance of `MtgJson[F]`
   */
  def apply[F[_]: Sync, Repr: Decoder](
    mtgjson: F[String]
  ): MtgJson[F, Repr] =
    new MtgJson[F, Repr] {
      def db: F[Either[Error, Repr]] =
        mtgjson.map(decode[Repr](_).left.map(circeErrorToMtgJsonError))
    }

  private[this] def circeErrorToMtgJsonError(error: CirceError): Error = error match {
    case e: CirceParsingFailure  => ParsingFailure(e.getMessage())
    case e: CirceDecodingFailure => DecodingFailure(e.getMessage())
  }
}
