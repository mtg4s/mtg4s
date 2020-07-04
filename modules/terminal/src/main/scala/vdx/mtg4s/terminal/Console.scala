package vdx.mtg4s.terminal

import cats.Show
import cats.effect.Sync
import cats.kernel.Eq
import cats.syntax.apply._
import vdx.mtg4s.terminal.{AutoCompletionConfig, AutoCompletionSource}

trait Console[F[_]] {
  def putStr(text: String): F[Unit]
  def putStrLn(): F[Unit]
  def putStrLn(text: String): F[Unit]
  def readLine(prompt: String): F[String]
  def readLine[Repr: Show: Eq](prompt: String, autocomplete: AutoCompletionSource[Repr])(
    implicit cfg: AutoCompletionConfig[Repr]
  ): F[(String, Option[Repr])]

  def clearScreen(): F[Unit]
  def moveToLastLine(): F[Unit]
}

object Console {
  def apply[F[_]: Sync](terminal: Terminal, lineReader: LineReader[F]): Console[F] =
    new Console[F] {
      def putStr(text: String): F[Unit] =
        write(text)

      def putStrLn(): F[Unit] =
        putStrLn("")

      def putStrLn(text: String): F[Unit] =
        write(s"$text\n")

      def readLine(prompt: String): F[String] =
        lineReader.readLine(prompt)

      def readLine[Repr: Show: Eq](prompt: String, autocomplete: AutoCompletionSource[Repr])(
        implicit cfg: AutoCompletionConfig[Repr]
      ): F[(String, Option[Repr])] =
        lineReader.readLine(prompt, autocomplete)

      def clearScreen(): F[Unit] =
        write(TerminalControl.clearScreen()) *> write(TerminalControl.move(1, 1))

      def moveToLastLine(): F[Unit] =
        write(TerminalControl.move(terminal.getHeight() - 1, 1))

      private def write(text: String): F[Unit] =
        Sync[F].delay(terminal.writer().write(text))
    }

  def apply[F[_]](implicit ev: Console[F]) = ev
}
