package lila
package model

case class History(
  lastMove: Option[(Pos, Pos)] = None,
  castles: Map[Color, (Boolean, Boolean)] = Map(
    White -> (false, false),
    Black -> (false, false)
  )
) {

  def isLastMove(p1: Pos, p2: Pos) = lastMove == (p1, p2)

  def canCastleKingSide(color: Color): Boolean = castles(color)._1
  def canCastleQueenSide(color: Color): Boolean = castles(color)._2
}
