package vdx.mtg4s

trait CardDB[F[_], Repr, SetId] {
  def findByNameAndSet(name: CardName, setId: SetId): F[Option[Repr]]
  def findMatchingByName(fragment: String): F[List[Repr]]
}
