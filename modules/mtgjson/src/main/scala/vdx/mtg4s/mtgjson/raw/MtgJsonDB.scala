package vdx.mtg4s.mtgjson.raw

import cats.Applicative
import cats.instances.option._
import cats.instances.string._
import cats.kernel.Eq
import cats.syntax.eq._
import cats.syntax.flatMap._
import monocle.Getter
import vdx.mtg4s._

object MtgJsonDB {

  /**
   * Creates card DB using AllPrintngs.json as a source and {{Card}} as a representation
   */
  def apply[F[_]: Applicative, SetId: Eq](
    allPrintings: AllPrintings
  )(implicit GSI: Getter[Set, SetId]): CardDB[F, Card, SetId] = new CardDB[F, Card, SetId] {

    def findMatchingByName(fragment: String): F[List[Card]] = ???

    def findByNameAndSet(name: CardName, setId: SetId): F[Option[Card]] =
      Applicative[F].pure(allPrintings.values.find(GSI.get(_) === setId) >>= findCardInSet(name))

    private[this] def findCardInSet(name: CardName)(set: Set): Option[Card] =
      set.cards.find(_.name === name)
  }
}
