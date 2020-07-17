package vdx.mtg4s.mtgjson

import java.io.{File, InputStream}

import awscala.s3.{Bucket, S3}
import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._

sealed trait FileMirror[F[_]] {
  def uploadFile(file: File, version: String): F[Unit]
  def downloadFile(version: String): Resource[F, Option[InputStream]]
}

object FileMirror {
  def apply[F[_]: Sync](prefix: String, ext: String): FileMirror[F] =
    new FileMirror[F] {
      implicit val region: awscala.Region = awscala.Region.EU_WEST_1

      val s3 = S3(sys.env("S3_ACCESS_KEY_ID"), sys.env("S3_SECRET_ACCESS_KEY"))

      def uploadFile(file: File, version: String): F[Unit] =
        withMtgJsonBucket(bucket => s3.put(bucket, mtgjsonFilename(version), file)).void

      def downloadFile(version: String): Resource[F, Option[InputStream]] =
        Resource.make(
          withMtgJsonBucket(bucket =>
            s3.getObject(bucket, mtgjsonFilename(version))
              .map(_.content)
          )
        )(maybeStream => maybeStream.fold(Sync[F].unit)(stream => Sync[F].delay(stream.close())))

      private def withMtgJsonBucket[A](f: Bucket => A): F[A] =
        Sync[F]
          .delay(s3.bucket(sys.env("S3_MTG4S_BUCKET")))
          .flatMap(
            _.fold(Sync[F].raiseError[A](new RuntimeException("Bucket not set")))(bucket => Sync[F].delay(f(bucket)))
          )

      private def mtgjsonFilename(version: String): String =
        s"$prefix-$version.$ext"
    }
}
