package vdx.mtg4s.mtgjson.raw

import cats.Applicative
import cats.instances.string._
import cats.kernel.Eq
import cats.syntax.eq._
import monocle.Getter
import vdx.mtg4s._

object MtgJsonDB {

  /**
   * Creates card DB using AllPrintngs.json as a source and {{Card}} as a representation
   */
  def apply[F[_]: Applicative, SetId: Eq](
    allPrintings: AllPrintings
  )(implicit GSI: Getter[Set, SetId]): CardDB[F, CardAndSet, SetId] =
    new CardDB[F, CardAndSet, SetId] {

      def findMatchingByName(fragment: String): F[List[CardAndSet]] =
        Applicative[F].pure(
          allPrintings.data.values.toList
            .map(set => set.cards.map(card => CardAndSet(card = card, set = set)))
            .flatten
            .filter(_.card.name.contains(fragment))
        )

      def findByNameAndSet(name: CardName, setId: SetId): F[Option[CardAndSet]] =
        Applicative[F].pure(allPrintings.data.values.find(GSI.get(_) === setId).flatMap(findCardInSet(name)))

      private[this] def findCardInSet(name: CardName)(set: Set): Option[CardAndSet] =
        set.cards.find(_.name === name).map(card => CardAndSet(card = card, set = set))
    }
}
