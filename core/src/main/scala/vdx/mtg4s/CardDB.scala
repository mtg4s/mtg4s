package vdx.mtg4s

trait CardDB[F[_], Repr] {
  def find(name: CardName, set: Set): F[Option[Repr]]
}
