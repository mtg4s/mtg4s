package vdx.mtg4s.terminal

import cats.effect.Sync
import cats.syntax.apply._
import vdx.mtg4s.terminal.LineReader.Autocomplete

trait Console[F[_]] {
  def putStrLn(): F[Unit]
  def putStrLn(text: String): F[Unit]
  def readLine(prompt: String): F[String]
  def readLine(prompt: String, autocomplete: Autocomplete[F]): F[String]

  def clearScreen(): F[Unit]
}

object Console {
  def apply[F[_]: Sync](terminal: Terminal, lineReader: LineReader[F]): Console[F] =
    new Console[F] {
      def putStrLn(): F[Unit] =
        putStrLn("")

      def putStrLn(text: String): F[Unit] =
        write(s"$text\n")

      def readLine(prompt: String): F[String] =
        lineReader.readLine(prompt)

      def readLine(prompt: String, autocomplete: Autocomplete[F]): F[String] =
        lineReader.readLine(prompt, autocomplete)

      def clearScreen(): F[Unit] =
        write(TerminalControl.clearScreen()) *> write(TerminalControl.move(1, 1))

      private def write(text: String): F[Unit] =
        Sync[F].delay(terminal.writer().write(text))
    }

  def apply[F[_]](implicit ev: Console[F]) = ev
}
