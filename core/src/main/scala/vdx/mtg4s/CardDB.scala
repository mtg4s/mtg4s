package vdx.mtg4s

import vdx.mtg4s.MtgSet.SetCode
import vdx.mtg4s.MtgSet.SetName

trait CardDB[F[_], Repr] {
  def findByNameAndSetCode(name: CardName, setCode: SetCode): F[Option[Repr]]

  def findByNameAndSetName(name: CardName, setName: SetName): F[Option[Repr]]
}
