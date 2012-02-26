package lila.chess
package model

import Pos.makePos
import scalaz.Success
import scala.math.{ min, max }

case class Actor(piece: Piece, pos: Pos, board: Board) {

  lazy val implications: Implications = kingSafety(piece.role match {

    case Bishop ⇒ longRange(Bishop.dirs)

    case Queen  ⇒ longRange(Queen.dirs)

    case Knight ⇒ shortRange(Knight.dirs)

    case King   ⇒ preventsCastle(shortRange(King.dirs)) ++ castle

    case Rook ⇒ (for {
      kingPos ← board kingPosOf color
      side ← Side.kingRookSide(kingPos, pos)
      if history canCastle color on side
    } yield history.withoutCastle(color, side)) map { nh ⇒
      longRange(Rook.dirs) mapValues (_ withHistory nh)
    } getOrElse longRange(Rook.dirs)

    case Pawn ⇒ pawnDir(pos) map { next ⇒
      val unmoved = if (color == White) pos.y == 2 else pos.y == 7
      val passable = if (color == White) pos.y == 5 else pos.y == 4
      val fwd = Some(next) filterNot board.occupations
      val moving = (pos2: Pos) ⇒ board.move(pos, pos2)
      def capture(horizontal: Direction): Option[Implication] = for {
        p ← horizontal(next); if enemies(p);
        b ← board.taking(pos, p)
      } yield (p, b)
      def enpassant(horizontal: Direction): Option[Implication] = for {
        victimPos ← horizontal(pos); if passable
        victim ← board(victimPos); if victim == !color - Pawn
        targetPos ← horizontal(next)
        victimFrom ← pawnDir(victimPos) flatMap pawnDir
        if history.lastMove == Some(victimFrom, victimPos)
        b ← board.taking(pos, targetPos, Some(victimPos))
      } yield (targetPos, b)
      List(
        for (p ← fwd; b ← moving(p)) yield (p, b),
        for {
          p ← fwd; if unmoved
          p2 ← pawnDir(p); if !(board occupations p2)
          b ← moving(p2)
        } yield (p2, b),
        capture(_.left),
        capture(_.right),
        enpassant(_.left),
        enpassant(_.right)
      ).flatten toMap
    } getOrElse Map.empty
  })

  lazy val moves: Set[Pos] = implications.keySet

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

  private def kingSafety(implications: Implications): Implications =
    implications filterNot {
      case (p, b) ⇒ b actorsOf !color exists { enemy ⇒
        b kingPosOf color map (enemy threatens _) getOrElse false
      }
    }

  private def castle: Implications = {

    lazy val enemyThreats = (board actorsOf !color).toSet flatMap { actor: Actor ⇒
      actor.threats
    }

    def on(side: Side): Option[Implication] = for {
      kingPos ← board kingPosOf color
      if history canCastle color on side
      tripToRook = side.tripToRook(kingPos, board)
      rookPos ← tripToRook.lastOption
      if board(rookPos) == Some(color.rook)
      newKingPos ← makePos(side.castledKingX, kingPos.y)
      securedPoss = kingPos <-> newKingPos
      if (enemyThreats & securedPoss.toSet).isEmpty
      newRookPos ← makePos(side.castledRookX, rookPos.y)
      b1 ← board take rookPos
      b2 ← b1.move(kingPos, newKingPos)
      b3 ← b2.place(color.rook, newRookPos)
    } yield (newKingPos, b3 updateHistory (_ withoutCastles color))

    List(on(KingSide), on(QueenSide)).flatten toMap
  }

  private def preventsCastle(implications: Implications) =
    if (history.canCastle(color).any) {
      val newHistory = history withoutCastles color
      implications mapValues (_ withHistory newHistory)
    }
    else implications

  private def shortRange(dirs: Directions): Implications = {
    (dirs map { _(pos) }).flatten filterNot friends map { to ⇒
      (if (enemies(to)) board.taking(pos, to) else board.move(pos, to)) map (to -> _)
    } flatten
  } toMap

  private def longRange(dirs: Directions): Implications = {

    def forward(p: Pos, dir: Direction): List[Implication] = dir(p) match {
      case None                        ⇒ Nil
      case Some(next) if friends(next) ⇒ Nil
      case Some(next) if enemies(next) ⇒ board.taking(pos, next) map { b ⇒
        (next, b)
      } toList
      case Some(next) ⇒ board.move(pos, next) map { b ⇒
        (next, b) :: forward(next, dir)
      } getOrElse Nil
    }

    (dirs flatMap { dir ⇒ forward(pos, dir) }) toMap
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

  private def history = board.history
  private def friends = board occupation color
  private def enemies = board occupation !color
  private val pawnDir: Direction = if (color == White) _.up else _.down
}
