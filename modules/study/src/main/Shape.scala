package lila.study

import chess.Pos

sealed trait Shape

object Shape {

  type ID = String
  type Brush = String

  case class Circle(brush: Brush, orig: Pos) extends Shape
  case class Arrow(brush: Brush, orig: Pos, dest: Pos) extends Shape
}
