package vdx.mtg4s.mtgjson

package object raw {
  type AllPrintings = Map[String, Set]

  type ListStringOrString = Either[List[String], String]
}
