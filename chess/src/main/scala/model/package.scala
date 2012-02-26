package lila.chess

package object model {

  type Direction = Pos => Option[Pos]
  type Directions = List[Direction]
}
