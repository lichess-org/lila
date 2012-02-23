package lila
package model

case class Piece(color: Color, role: Role) {

  def moves(pos: Pos, board: Board): Set[Pos] = {
    Set.empty
  }

  def is(c: Color) = c == color
  def is(r: Role) = r == role

  def forsyth: Char = if (color == White) role.forsyth.toUpper else role.forsyth

  override def toString = (color + " " + role).toLowerCase
}
