package lila

package object model {

  type History = List[(Pos, Pos)]

  type Direction = Pos => Option[Pos]
}
