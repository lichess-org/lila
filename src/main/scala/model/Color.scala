package lila
package model

sealed trait Color {
  def -(role: Role) = Piece(this, role)
}
case object White extends Color
case object Black extends Color

object Color {
  def apply(b: Boolean): Color = if (b) White else Black
}
