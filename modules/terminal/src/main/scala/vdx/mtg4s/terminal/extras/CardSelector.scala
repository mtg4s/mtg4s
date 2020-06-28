package vdx.mtg4s.terminal.extras

import cats.kernel.Eq
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{~>, Id, Monad, Show}
import monocle.Getter
import vdx.mtg4s.terminal.AutoCompletionConfig._
import vdx.mtg4s.terminal.Console
import vdx.mtg4s.{CardDB, CardName}

trait CardSelector[F[_], Repr] {
  def run[SetId](cardDb: CardDB[F, Repr, SetId]): F[Option[Repr]]
}

object CardSelector {
  def apply[F[_]: Monad: Console, Repr: Show: Eq: Getter[*, CardName]](tf: F ~> Id): CardSelector[F, Repr] =
    new CardSelector[F, Repr] {
      def run[SetId](cardDb: CardDB[F, Repr, SetId]): F[Option[Repr]] = {
        val console = Console[F]
        val acSource = CardNameAutoCompletionSource[F, Repr, SetId](cardDb, tf)
        for {
          _      <- console.clearScreen()
          _      <- console.moveToLastLine()
          result <- console.readLine("Card name: ", acSource)
        } yield result._2

      }
    }
}
