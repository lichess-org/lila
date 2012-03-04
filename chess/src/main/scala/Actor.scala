package lila.chess

import Pos.posAt

case class Actor(piece: Piece, pos: Pos, board: Board) {

  lazy val moves: List[Move] = kingSafety(piece.role match {

    case Bishop ⇒ longRange(Bishop.dirs)

    case Queen  ⇒ longRange(Queen.dirs)

    case Knight ⇒ shortRange(Knight.dirs)

    case King   ⇒ preventsCastle(shortRange(King.dirs)) ++ castle

    case Rook ⇒ (for {
      kingPos ← board kingPosOf color
      side ← Side.kingRookSide(kingPos, pos)
      if history canCastle color on side
    } yield history.withoutCastle(color, side)) map { nh ⇒
      longRange(Rook.dirs) map (_ withHistory nh)
    } getOrElse longRange(Rook.dirs)

    case Pawn ⇒ pawnDir(pos) map { next ⇒
      val fwd = Some(next) filterNot board.occupations
      def capture(horizontal: Direction): Option[Move] = for {
        p ← horizontal(next); if enemies(p);
        b ← board.taking(pos, p)
      } yield move(p, b, Some(p))
      def enpassant(horizontal: Direction): Option[Move] = for {
        victimPos ← horizontal(pos); if pos.y == color.passablePawnY
        victim ← board(victimPos); if victim == !color - Pawn
        targetPos ← horizontal(next)
        victimFrom ← pawnDir(victimPos) flatMap pawnDir
        if history.lastMove == Some(victimFrom, victimPos)
        b ← board.taking(pos, targetPos, Some(victimPos))
      } yield move(targetPos, b, enpassant = true)
      def forward(p: Pos): Option[Move] =
        if (pos.y == color.promotablePawnY)
          board.promote(pos, p) map { b ⇒ move(p, b, promotion = Some(Queen)) }
        else
          board.move(pos, p) map { b ⇒ move(p, b) }
      List(
        for {
          p ← fwd
          m ← forward(p)
        } yield m,
        for {
          p ← fwd; if pos.y == color.unmovedPawnY
          p2 ← pawnDir(p); if !(board occupations p2)
          b ← board.move(pos, p2)
        } yield move(p2, b),
        capture(_.left),
        capture(_.right),
        enpassant(_.left),
        enpassant(_.right)
      ).flatten
    } getOrElse Nil
  })

  lazy val destinations: List[Pos] = moves map (_.dest)

  def color = piece.color
  def is(c: Color) = c == piece.color
  def is(p: Piece) = p == piece

  def threatens(to: Pos): Boolean = enemies(to) && threats(to)

  lazy val threats: Set[Pos] = piece.role match {
    case Pawn ⇒ pawnDir(pos) map { next ⇒
      Set(next.left, next.right) flatten
    } getOrElse Set.empty
    case Queen | Bishop | Rook ⇒ longRangePoss(piece.role.dirs) toSet
    case role                  ⇒ (role.dirs map { d ⇒ d(pos) }).flatten toSet
  }

  def hash: String = piece.forsyth + pos.key

  private def kingSafety(ms: List[Move]): List[Move] =
    ms filterNot { m ⇒
      m.after actorsOf !color exists { enemy ⇒
        m.after kingPosOf color map (enemy threatens _) getOrElse false
      }
    }

  private def castle: List[Move] = {

    lazy val enemyThreats = (board actorsOf !color).toSet flatMap { actor: Actor ⇒
      actor.threats
    }

    def on(side: Side): Option[Move] = for {
      kingPos ← board kingPosOf color
      if history canCastle color on side
      tripToRook = side.tripToRook(kingPos, board)
      rookPos ← tripToRook.lastOption
      if board(rookPos) == Some(color.rook)
      newKingPos ← posAt(side.castledKingX, kingPos.y)
      securedPoss = kingPos <-> newKingPos
      if (enemyThreats & securedPoss.toSet).isEmpty
      newRookPos ← posAt(side.castledRookX, rookPos.y)
      b1 ← board take rookPos
      b2 ← b1.move(kingPos, newKingPos)
      b3 ← b2.place(color.rook, newRookPos)
      b4 = b3 updateHistory (_ withoutCastles color)
    } yield move(newKingPos, b4, castle = Some((rookPos, newRookPos)))

    List(on(KingSide), on(QueenSide)).flatten
  }

  private def preventsCastle(ms: List[Move]) =
    if (history.canCastle(color).any) {
      val newHistory = history withoutCastles color
      ms map (_ withHistory newHistory)
    }
    else ms

  private def shortRange(dirs: Directions): List[Move] =
    (dirs map { _(pos) }).flatten filterNot friends map { to ⇒
      if (enemies(to)) board.taking(pos, to) map { move(to, _, Some(to)) }
      else board.move(pos, to) map { move(to, _) }
    } flatten

  private def longRange(dirs: Directions): List[Move] = {

    def forward(p: Pos, dir: Direction): List[Move] = dir(p) match {
      case None                        ⇒ Nil
      case Some(next) if friends(next) ⇒ Nil
      case Some(next) if enemies(next) ⇒ board.taking(pos, next) map { b ⇒
        move(next, b, Some(pos))
      } toList
      case Some(next) ⇒ board.move(pos, next) map { b ⇒
        move(next, b) :: forward(next, dir)
      } getOrElse Nil
    }

    dirs flatMap { dir ⇒ forward(pos, dir) }
  }

  private def longRangePoss(dirs: Directions): List[Pos] = {

    def forward(p: Pos, dir: Direction): List[Pos] = dir(p) match {
      case None                        ⇒ Nil
      case Some(next) if friends(next) ⇒ Nil
      case Some(next) if enemies(next) ⇒ List(next)
      case Some(next)                  ⇒ next :: forward(next, dir)
    }

    dirs flatMap { dir ⇒ forward(pos, dir) }
  }

  private def move(
    dest: Pos,
    after: Board,
    capture: Option[Pos] = None,
    castle: Option[(Pos, Pos)] = None,
    promotion: Option[PromotableRole] = None,
    enpassant: Boolean = false) = Move(
    piece = piece,
    orig = pos,
    dest = dest,
    before = board,
    after = after,
    capture = capture,
    castle = castle,
    promotion = promotion,
    enpassant = enpassant)

  private def history = board.history
  private def friends = board occupation color
  private def enemies = board occupation !color
  private lazy val pawnDir: Direction = if (color == White) _.up else _.down
}
