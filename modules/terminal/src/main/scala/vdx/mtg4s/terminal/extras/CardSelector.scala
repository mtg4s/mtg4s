package vdx.mtg4s.terminal.extras

import cats.kernel.Eq
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{~>, Id, Monad, Show}
import com.gaborpihaj.console4s.{AutoCompletion, Console}
import monocle.Getter
import vdx.mtg4s.{CardDB, CardName}

trait CardSelector[F[_], Repr] {
  def run[SetId](cardDb: CardDB[F, Repr, SetId]): F[Option[Repr]]
}

object CardSelector {
  def apply[F[_]: Monad: Console, Repr: Show: Eq: Getter[*, CardName]](tf: F ~> Id): CardSelector[F, Repr] =
    new CardSelector[F, Repr] {
      def run[SetId](cardDb: CardDB[F, Repr, SetId]): F[Option[Repr]] = {
        val console = Console[F]
        val autoCompletion = AutoCompletion(
          source = CardNameAutoCompletionSource[F, Repr, SetId](cardDb, tf),
          config = AutoCompletion.defaultAutoCompletionConfig
        )
        for {
          _      <- console.clearScreen()
          _      <- console.moveToLastLine()
          result <- console.readLine("Card name: ", autoCompletion)
        } yield result._2

      }
    }
}
