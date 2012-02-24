package lila
package model

import scalaz.Success

case class Actor(piece: Piece, pos: Pos, board: Board) {

  def implications: Implications = kingSafety(piece.role match {
    case Pawn ⇒ dir(pos) map { next ⇒
      val unmoved = if (color == White) pos.y == 2 else pos.y == 7
      val passable = if (color == White) pos.y == 5 else pos.y == 4
      val one = Some(next) filterNot board.occupations
      val moving = board move pos
      def capture(horizontal: Direction): Option[Implication] = for {
        p ← horizontal(next); if (enemies(p));
        b1 ← (board take p toOption)
        b2 ← b1 move pos toOption p
      } yield (p, b2)
      def enpassant(horizontal: Direction): Option[Implication] = for {
        victimPos ← horizontal(pos); if (passable)
        victim ← board(victimPos); if victim == !color - Pawn
        targetPos ← horizontal(next)
        victimFrom ← dir(victimPos) flatMap dir
        if board.history.lastMove == Some(victimFrom, victimPos)
        b1 ← moving toOption targetPos
        b2 ← (b1 take victimPos toOption)
      } yield (targetPos, b2)
      List(
        for (p ← one; b ← moving toOption p) yield (p, b),
        for {
          p ← one; if (unmoved)
          p2 ← dir(p); if (!board.occupations(p2))
          b ← moving toOption p2
        } yield (p2, b),
        capture(_.left),
        capture(_.right),
        enpassant(_.left),
        enpassant(_.right)
      ).flatten toMap
    } getOrElse Map.empty

    case r if (r.trajectory) ⇒ implicationTrajectories(r.dirs, pos)

    case role ⇒ {
      val tos: Set[Pos] = (role.dirs map { d ⇒ d(pos) }).flatten.toSet -- friends
      (tos map { to: Pos ⇒
        val nboard =
          if (enemies(to)) board take to flatMap (_ move pos to to)
          else board move pos to to
        nboard.toOption map (to -> _)
      }).flatten toMap
    }
  })

  def moves: Set[Pos] = implications.keySet

  // can it threaten the opponent king?
  def threatens(to: Pos): Boolean = enemies(to) && threats(to)

  def threats: Set[Pos] = piece.role match {
    case Pawn ⇒ dir(pos) map { next ⇒
      Set(next.left, next.right) flatten
    } getOrElse Set.empty
    case r if (r.trajectory) ⇒ posTrajectories(r.dirs, pos)
    case r if (r.threatens)  ⇒ (r.dirs map { d ⇒ d(pos) }).flatten.toSet
    case _                   ⇒ Set()
  }

  def color = piece.color
  def is(c: Color) = c == piece.color
  def friends: Set[Pos] = board occupation color
  def enemies: Set[Pos] = board occupation !color
  def dir: Direction = if (color == White) _.up else _.down

  private def kingSafety(implications: Implications): Implications = {
    implications filterNot {
      case (pos, nboard) ⇒ nboard actorsOf !color exists { enemy ⇒
        nboard kingPosOf color map (enemy threatens _) getOrElse false
      }
    }
  }

  private def posTrajectories(dirs: Directions, from: Pos): Set[Pos] = {

    def forward(p: Pos, dir: Direction): List[Pos] = dir(p) match {
      case None                        ⇒ Nil
      case Some(next) if friends(next) ⇒ Nil
      case Some(next) if enemies(next) ⇒ List(next)
      case Some(next)                  ⇒ next :: forward(next, dir)
    }

    dirs flatMap { dir ⇒ forward(from, dir) } toSet
  }

  private def implicationTrajectories(dirs: Directions, from: Pos): Implications = {

    val moving = board move from

    def forward(p: Pos, dir: Direction): List[Implication] = dir(p) match {
      case None                        ⇒ Nil
      case Some(next) if friends(next) ⇒ Nil
      case Some(next) if enemies(next) ⇒ (for {
        b1 ← (board take next toOption)
        b2 ← b1 move from toOption next
      } yield next -> b2) toList
      case Some(next) ⇒ moving toOption next map { b =>
        (next, b) :: forward(next, dir)
      } getOrElse forward(next, dir)
    }

    (dirs flatMap { dir ⇒ forward(from, dir) }) toMap
  }
}
