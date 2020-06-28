package vdx.mtg4s.terminal.extras

import cats.{~>, Id}
import monocle.Getter
import vdx.mtg4s.terminal.AutoCompletionSource
import vdx.mtg4s.{CardDB, CardName}

object CardNameAutoCompletionSource {
  def apply[F[_], Repr, SetId](
    cardDb: CardDB[F, Repr, SetId],
    tf: F ~> Id // This needs some rethinking
  )(implicit G: Getter[Repr, CardName]): AutoCompletionSource[Repr] =
    new AutoCompletionSource[Repr] {
      def candidates(fragment: String): List[(String, Repr)] = {
        val (startsWith, contains) =
          tf(cardDb.findMatchingByName(fragment))
            .partition(G.get(_).startsWith(fragment))

        (startsWith.sortWith(G.get(_) < G.get(_)) ++ contains.sortWith(G.get(_) < G.get(_)))
          .map(card => G.get(card) -> card)
      }
    }
}
