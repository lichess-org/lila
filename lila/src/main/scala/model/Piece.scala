package lila
package model

sealed trait Role {
  val forsyth: Char
}
case object King extends Role {
  val forsyth = 'k'
}
case object Queen extends Role {
  val forsyth = 'q'
}
case object Rook extends Role {
  val forsyth = 'r'
}
case object Bishop extends Role {
  val forsyth = 'b'
}
case object Knight extends Role {
  val forsyth = 'n'
}
case object Pawn extends Role {
  val forsyth = 'p'
}

object Role {
  val all = List(King, Queen, Rook, Bishop, Knight, Pawn)
}

sealed trait Color {
  def -(role: Role) = Piece(this, role)
}
case object White extends Color
case object Black extends Color

object Color {
  def apply(b: Boolean): Color = if (b) White else Black
}

case class Piece(color: Color, role: Role) {

  import Pos._

  override def toString = (color + " " + role).toLowerCase

  def forsyth: Char = if (color == White) role.forsyth.toUpper else role.forsyth
}
