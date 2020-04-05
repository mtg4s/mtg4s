package vdx

import shapeless.tag
import shapeless.tag.@@

import java.util.UUID

package object mtg4s extends Tags {

  type CardName = String @@ CardNameTag
  object CardName {
    def apply(name: String): CardName = tag[CardNameTag][String](name)
  }

  type MtgJsonId = UUID @@ MtgJsonIdTag
  object MtgJsonId {
    def apply(id: UUID): MtgJsonId = tag[MtgJsonIdTag][UUID](id)
  }

  type Set = String @@ SetTag
  object Set {
    def apply(code: String): Set = tag[SetTag][String](code)
  }
}
