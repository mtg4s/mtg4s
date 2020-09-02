package vdx.mtg4s.mtgjson.raw

import cats.Applicative
import cats.instances.string._
import cats.kernel.Eq
import cats.syntax.eq._
import monocle.Getter
import vdx.mtg4s._
import vdx.mtg4s.mtgjson.MtgJsonId

trait MtgJsonDB[F[_], RCard, RSetId] extends CardDB[F, RCard, RSetId] {
  def findByMtgJsonId(id: MtgJsonId): F[Option[RCard]]
}

object MtgJsonDB {

  /**
   * Creates card DB using AllPrintngs.json as a source and {{Card}} as a representation
   */
  def apply[F[_]: Applicative, SetId: Eq](
    allPrintings: AllPrintings
  )(implicit GSI: Getter[Set, SetId]): MtgJsonDB[F, CardAndSet, SetId] =
    new MtgJsonDB[F, CardAndSet, SetId] {

      def findMatchingByName(fragment: String): F[List[CardAndSet]] =
        Applicative[F].pure(
          allPrintings.data.values.toList
            .map(set => set.cards.map(card => CardAndSet(card = card, set = set)))
            .flatten
            .filter(_.card.name.toLowerCase().contains(fragment.toLowerCase()))
        )

      def findByNameAndSet(name: CardName, setId: SetId): F[Option[CardAndSet]] =
        Applicative[F].pure(allPrintings.data.values.find(GSI.get(_) === setId).flatMap(findCardInSet(name)))

      def findByMtgJsonId(id: MtgJsonId): F[Option[CardAndSet]] =
        Applicative[F].pure(allPrintings.data.values.toList.collectFirst {
          case set if findCardInSet(id)(set).isDefined => findCardInSet(id)(set)
        }.flatten)

      private[this] def findCardInSet(name: CardName)(set: Set): Option[CardAndSet] =
        set.cards.find(_.name === name).map(card => CardAndSet(card = card, set = set))

      private[this] def findCardInSet(id: MtgJsonId)(set: Set): Option[CardAndSet] =
        set.cards.find(_.uuid === id).map(card => CardAndSet(card = card, set = set))

    }
}
