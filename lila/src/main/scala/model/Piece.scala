package lila
package model

sealed trait Role
case object King extends Role
case object Queen extends Role
case object Rook extends Role
case object Bishop extends Role
case object Knight extends Role
case object Pawn extends Role

sealed trait Color {
  def -(role: Role) = Piece(this, role)
}
case object White extends Color
case object Black extends Color

case class Piece(color: Color, role: Role) {

  import Pos._

  override def toString = (color + " " + role).toLowerCase
}
