package lila
package model

case class History(
    lastMove: Option[(Pos, Pos)] = None,
    castles: Map[Color, (Boolean, Boolean)] = Map()) {

  def isLastMove(p1: Pos, p2: Pos) = lastMove == (p1, p2)

  def canCastle(color: Color) = new {
    def on(side: Side): Boolean = (castles get color, side) match {
      case (None, _)                     ⇒ false
      case (Some((king, _)), KingSide)   ⇒ king
      case (Some((_, queen)), QueenSide) ⇒ queen
    }
  }
}

object History {

  def castle(color: Color, kingSide: Boolean, queenSide: Boolean) = History(
    castles = Map(color -> (kingSide, queenSide))
  )
}
