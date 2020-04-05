package vdx.mtg4s.mtgjson

package object raw {

  /**
   * The full representation of the AllPrintings.json database
   */
  type AllPrintings = Map[String, Set]

  type ListStringOrString = Either[List[String], String]
}
