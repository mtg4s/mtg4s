package vdx.mtg4s

import shapeless.tag
import shapeless.tag.@@
import vdx.mtg4s.MtgSet.{SetCode, SetName}

case class MtgSet(name: SetName, code: SetCode)

object MtgSet extends Tags {
  type SetName = String @@ SetNameTag
  object SetName {
    def apply(code: String): SetName = tag[SetNameTag][String](code)
  }

  type SetCode = String @@ SetCodeTag
  object SetCode {
    def apply(code: String): SetCode = tag[SetCodeTag][String](code)
  }

}
