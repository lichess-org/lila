package lila
package model

import scalaz.Success

case class Actor(piece: Piece, pos: Pos, board: Board) {

  lazy val implications: Implications = kingSafety(piece.role match {
    case Pawn ⇒ dir(pos) map { next ⇒
      val unmoved = if (color == White) pos.y == 2 else pos.y == 7
      val passable = if (color == White) pos.y == 5 else pos.y == 4
      val one = Some(next) filterNot board.occupations
      val moving = (pos2: Pos) ⇒ board.move(pos, pos2)
      def capture(horizontal: Direction): Option[Implication] = for {
        p ← horizontal(next); if (enemies(p));
        b ← board.taking(pos, p)
      } yield (p, b)
      def enpassant(horizontal: Direction): Option[Implication] = for {
        victimPos ← horizontal(pos); if (passable)
        victim ← board(victimPos); if victim == !color - Pawn
        targetPos ← horizontal(next)
        victimFrom ← dir(victimPos) flatMap dir
        if board.history.lastMove == Some(victimFrom, victimPos)
        b ← board.taking(pos, targetPos, Some(victimPos))
      } yield (targetPos, b)
      List(
        for (p ← one; b ← moving(p)) yield (p, b),
        for {
          p ← one; if (unmoved)
          p2 ← dir(p); if (!board.occupations(p2))
          b ← moving(p2)
        } yield (p2, b),
        capture(_.left),
        capture(_.right),
        enpassant(_.left),
        enpassant(_.right)
      ).flatten toMap
    } getOrElse Map.empty

    case r if (r.trajectory) ⇒ implicationTrajectories(r.dirs, pos)

    case role ⇒ {
      (role.dirs map { d ⇒ d(pos) }).flatten filterNot friends map { to ⇒
        (if (enemies(to)) board.taking(pos, to) else board.move(pos, to)) map (to -> _)
      } flatten
    } toMap
  })

  lazy val moves: Set[Pos] = implications.keySet

  def color = piece.color
  def is(c: Color) = c == piece.color
  def friends: Set[Pos] = board occupation color
  def enemies: Set[Pos] = board occupation !color
  def dir: Direction = if (color == White) _.up else _.down

  private def kingSafety(implications: Implications): Implications =
    implications filterNot {
      case (p, b) ⇒ b actorsOf !color exists { enemy ⇒
        b kingPosOf color map (enemy threatens _) getOrElse false
      }
    }

  // can it threaten the opponent king?
  def threatens(to: Pos): Boolean = enemies(to) && ((piece.role match {
    case Pawn ⇒ dir(pos) map { next ⇒
      List(next.left, next.right) flatten
    } getOrElse Nil
    case role if (role.trajectory) ⇒ posTrajectories(role.dirs, pos)
    case role                      ⇒ (role.dirs map { d ⇒ d(pos) }).flatten
  }) contains to)

  private def posTrajectories(dirs: Directions, from: Pos): List[Pos] = {

    def forward(p: Pos, dir: Direction): List[Pos] = dir(p) match {
      case None                        ⇒ Nil
      case Some(next) if friends(next) ⇒ Nil
      case Some(next) if enemies(next) ⇒ List(next)
      case Some(next)                  ⇒ next :: forward(next, dir)
    }

    dirs flatMap { dir ⇒ forward(from, dir) }
  }

  private def implicationTrajectories(dirs: Directions, from: Pos): Implications = {

    val moving = (to: Pos) ⇒ board.move(from, to)

    def forward(p: Pos, dir: Direction): List[Implication] = dir(p) match {
      case None                        ⇒ Nil
      case Some(next) if friends(next) ⇒ Nil
      case Some(next) if enemies(next) ⇒ board.taking(from, next) map { b ⇒
        (next, b)
      } toList
      case Some(next) ⇒ moving(next) map { b ⇒
        (next, b) :: forward(next, dir)
      } getOrElse Nil
    }

    (dirs flatMap { dir ⇒ forward(from, dir) }) toMap
  }
}
