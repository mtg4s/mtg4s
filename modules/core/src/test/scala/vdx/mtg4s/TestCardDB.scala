package vdx.mtg4s

import java.util.UUID

import cats.kernel.Eq
import cats.{Applicative, Show}
import monocle.Getter

object TestCardDB {
  case class Card(name: String, set: SetName, id: UUID)

  object Card {
    implicit val show: Show[Card] = _.toString
    implicit val eq: Eq[Card] = Eq.fromUniversalEquals
    implicit val nameGetter: Getter[Card, CardName] = (card: Card) => CardName(card.name)
  }

  val defaultCards: Map[String, Card] = Map(
    "Primeval Titan - Iconic Masters" -> Card("Primeval Titan", SetName("Iconic Masters"), UUID.randomUUID()),
    "Primeval Titan - Magic 2011" -> Card("Primeval Titan", SetName("Magic 2011"), UUID.randomUUID()),
    "Snapcaster Mage - Innistrad" -> Card("Snapcaster Mage", SetName("Innistrad"), UUID.randomUUID()),
    "Lightning Bolt - Masters 25" -> Card("Lightning Bolt", SetName("Masters 25"), UUID.randomUUID()),
    "Karn, the Great Creator - War of the Spark" -> Card(
      "Karn, the Great Creator",
      SetName("War of the Spark"),
      UUID.randomUUID()
    )
  )

  def apply[F[_]: Applicative](cards: Map[String, Card] = defaultCards): CardDB[F, Card, SetName] =
    new CardDB[F, Card, SetName] {

      def findMatchingByName(fragment: String): F[List[Card]] =
        Applicative[F].pure(
          cards.filter {
            case (name, _) => name.toLowerCase().contains(fragment.toLowerCase())
          }.values.toList
        )

      def findByNameAndSet(name: CardName, set: SetName): F[Option[Card]] =
        Applicative[F].pure(cards.get(s"$name - $set").filter(card => SetName.eq.eqv(card.set, set)))
    }
}
