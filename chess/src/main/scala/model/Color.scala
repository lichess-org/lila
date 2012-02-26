package lila.chess
package model

sealed trait Color {

  def -(role: Role) = Piece(this, role)

  def unary_! : Color

  def pawn   = this - Pawn
  def bishop = this - Bishop
  def knight = this - Knight
  def rook   = this - Rook
  def queen  = this - Queen
  def king   = this - King
}

case object White extends Color {

  lazy val unary_! = Black
}

case object Black extends Color {

  lazy val unary_! = White
}

object Color {

  def apply(b: Boolean): Color = if (b) White else Black

  def all = List(White, Black)
}
