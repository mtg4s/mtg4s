package vdx.mtg4s.terminal

import cats.effect.Sync
import cats.{Eq, Show}
import vdx.mtg4s.terminal.linereader.LineReaderImpl

trait InputReader

trait LineReader[F[_]] {
  def readLine(prompt: String): F[String]
  def readLine[Repr: Show: Eq](prompt: String, autocomplete: AutoCompletionSource[Repr])(
    implicit cfg: AutoCompletionConfig[Repr]
  ): F[(String, Option[Repr])]
}

object LineReader {
  def apply[F[_]: Sync](terminal: Terminal): LineReader[F] = LineReaderImpl(terminal)
}
