package lila.chess
package model

sealed trait Color {

  def -(role: Role) = Piece(this, role)

  val unary_! : Color

  val unmovedPawnY: Int
  val passablePawnY: Int
  val promotablePawnY: Int

  def pawn   = this - Pawn
  def bishop = this - Bishop
  def knight = this - Knight
  def rook   = this - Rook
  def queen  = this - Queen
  def king   = this - King
}

case object White extends Color {

  lazy val unary_! = Black

  val unmovedPawnY = 2
  val passablePawnY = 5
  val promotablePawnY = 7
}

case object Black extends Color {

  lazy val unary_! = White

  val unmovedPawnY = 7
  val passablePawnY = 4
  val promotablePawnY = 2
}

object Color {

  def apply(b: Boolean): Color = if (b) White else Black

  def all = List(White, Black)
}
