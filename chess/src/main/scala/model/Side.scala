package lila.chess
package model

sealed trait Side {

  def castledKingX: Int
  def castledRookX: Int

  def tripToRook: (Pos, Board) ⇒ List[Pos]
}

object Side {

  lazy val all = List(KingSide, QueenSide)

  def kingRookSide(kingPos: Pos, rookPos: Pos): Option[Side] =
    if (kingPos.y == rookPos.y)
      Some(if (kingPos.x > rookPos.x) QueenSide else KingSide)
    else None
}

case object KingSide extends Side {

  val castledKingX = 7
  val castledRookX = 6

  val tripToRook: (Pos, Board) ⇒ List[Pos] = (pos, board) ⇒ pos >| board.occupations
}
case object QueenSide extends Side {

  val castledKingX = 3
  val castledRookX = 4

  val tripToRook: (Pos, Board) ⇒ List[Pos] = (pos, board) ⇒ pos |< board.occupations
}
