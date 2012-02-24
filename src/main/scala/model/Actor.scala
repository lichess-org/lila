package lila
package model

case class Actor(piece: Piece, pos: Pos, board: Board) {

  def moves: Set[Pos] = implications.keySet

  def implications: Map[Pos, Board] = {
    val tos: Set[Pos] = piece.role match {
      case Pawn ⇒ {
        dir(pos) map { next ⇒
          val unmoved = if (color == White) pos.y == 2 else pos.y == 7
          val passable = if (color == White) pos.y == 5 else pos.y == 4
          val one = Some(next) filterNot board.occupations
          def enpassant(horizontal: Direction): Option[Pos] = for {
            victimPos ← horizontal(pos)
            victim ← board(victimPos)
            if victim == !color - Pawn
            targetPos ← horizontal(next)
            victimFrom ← dir(victimPos) flatMap dir
            if board.history.lastMove == Some(victimFrom, victimPos)
          } yield targetPos
          Set(
            one,
            if (unmoved) one flatMap { o ⇒ dir(o) filterNot board.occupations }
            else None,
            next.left filter enemies,
            next.right filter enemies,
            if (passable) enpassant(_.left) else None,
            if (passable) enpassant(_.right) else None
          ) flatten
        } getOrElse Set.empty
      }
      case r if (r.trajectory) ⇒ trajectories(r.dirs, pos)
      case role                ⇒ (role.dirs map { d ⇒ d(pos) }).flatten.toSet -- friends
    }
    val implications: Map[Pos, Board] = piece.role match {
      case Pawn ⇒ tos map (_ -> board) toMap
      case role ⇒ (tos map { to: Pos ⇒
        val nboard =
          if (enemies(to)) board take to flatMap (_ move pos to to)
          else board move pos to to
        nboard.toOption map (to -> _)
      }).flatten toMap
    }

    kingSafety(implications)
  }

  def kingSafety(implications: Map[Pos, Board]): Map[Pos, Board] = {
    implications filterNot {
      case (pos, nboard) ⇒ nboard actorsOf !color exists { enemy ⇒
        nboard kingPosOf color map (enemy threatens _) getOrElse false
      }
    }
  }

  // can it threaten the opponent king?
  def threatens(to: Pos): Boolean =
    if (enemies(to)) {
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
    else false

  def color = piece.color
  def is(c: Color) = c == piece.color
  def friends: Set[Pos] = board occupation color
  def enemies: Set[Pos] = board occupation !color
  def dir: Direction = if (color == White) _.up else _.down

  private def trajectories(dirs: List[Direction], p: Pos): Set[Pos] = {

    def forward(p: Pos, dir: Direction): List[Pos] = dir(p) match {
      case None                        ⇒ Nil
      case Some(next) if friends(next) ⇒ Nil
      case Some(next) if enemies(next) ⇒ List(next)
      case Some(next)                  ⇒ next :: forward(next, dir)
    }

    dirs flatMap { dir ⇒ forward(p, dir) } toSet
  }
}
