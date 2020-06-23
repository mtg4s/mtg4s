package vdx

import cats.kernel.Eq
import shapeless.tag
import shapeless.tag.@@

package object mtg4s extends Tags {
  type CardName = String @@ CardNameTag
  object CardName {
    def apply(name: String): CardName = tag[CardNameTag][String](name)
  }

  type SetName = String @@ SetNameTag
  object SetName {
    def apply(code: String): SetName = tag[SetNameTag][String](code)
    implicit val eq: Eq[SetName] = Eq.fromUniversalEquals
  }

  type SetCode = String @@ SetCodeTag
  object SetCode {
    def apply(code: String): SetCode = tag[SetCodeTag][String](code)
  }
}
