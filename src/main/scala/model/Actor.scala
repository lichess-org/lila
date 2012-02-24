package lila
package model

final class Actor(piece: Piece, pos: Pos, board: Board, history: History = Nil) {

  def moves: Set[Pos] = piece.role match {
    case Pawn ⇒ {
      dir(pos) map { next ⇒
        val unmoved = (color == White && pos.y == 2) || (color == Black && pos.y == 7)
        val one = Some(next) filterNot board.occupations
        Set(
          one,
          if (unmoved) one flatMap { o ⇒ dir(o) filterNot board.occupations }
          else None,
          next.left filter enemies,
          next.right filter enemies
        ) flatten
      } getOrElse Set.empty
    }
    case r if (r.trajectory) ⇒ (new Trajectories(r.dirs, friends, enemies)) from pos
    case r                   ⇒ (r.dirs map { d ⇒ d(pos) }).flatten.toSet -- friends
  }

  // does it threaten the opponent king?
  def threatens(to: Pos): Boolean = {

    val poss: Set[Pos] = piece.role match {
      case Pawn ⇒ dir(pos) map { next ⇒
        Set(next.left, next.right) flatten
      } getOrElse Set.empty
      case r if (r.trajectory) ⇒ {
        new Trajectories(r.dirs, board occupation color, board occupation !color)
      } from pos
      case r if (r.threatens) ⇒ (r.dirs map { d ⇒ d(pos) }).flatten.toSet
      case _                  ⇒ Set()
    }
    poss(to)
  }

  def color = piece.color
  def is(c: Color) = c == piece.color
  def friends = board occupation color
  def enemies = board occupation !color
  def dir: Direction = if (color == White) _.up else _.down
}
