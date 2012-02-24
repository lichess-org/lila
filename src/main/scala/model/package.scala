package lila

package object model {

  type Direction = Pos => Option[Pos]
  type Directions = List[Direction]

  type Implication = (Pos, Board)
  type Implications = Map[Pos, Board]
}
