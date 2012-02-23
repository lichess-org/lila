package lila
package model

sealed trait Color {

  def -(role: Role) = Piece(this, role)

  val opposite: Color

  val unary_! = opposite
}

case object White extends Color {

  val opposite = Black
}

case object Black extends Color {

  val opposite = White
}

object Color {

  def apply(b: Boolean): Color = if (b) White else Black

  def all = List(White, Black)
}
