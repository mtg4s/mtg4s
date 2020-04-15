package vdx.mtg4s

import cats.data.Chain
import vdx.mtg4s.CardList.Card

case class CardList[CardId](cards: Chain[Card[CardId]])

object CardList {
  case class Card[CardId](id: CardId, count: Int)
}
