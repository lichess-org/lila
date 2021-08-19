package shogi

import format.Uci

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

final case class Actor(
    piece: Piece,
    pos: Pos,
    board: Board
) {

  lazy val moves: List[Move] = kingSafetyMoveFilter(trustedMoves())

  /** The moves without taking defending the king into account */
  def trustedMoves(): List[Move] = {
    val moves = piece.role match {
      case Pawn if piece.color == Sente => shortRange(Pawn.dirs)
      case Pawn                         => shortRange(Pawn.dirsOpposite)

      case Lance if piece.color == Sente => longRange(Lance.dirs)
      case Lance                         => longRange(Lance.dirsOpposite)

      case Gold if piece.color == Sente => shortRange(Gold.dirs)
      case Gold                         => shortRange(Gold.dirsOpposite)

      case Silver if piece.color == Sente => shortRange(Silver.dirs)
      case Silver                         => shortRange(Silver.dirsOpposite)

      case Bishop => longRange(Bishop.dirs)

      case Rook => longRange(Rook.dirs)

      case Knight if piece.color == Sente => shortRange(Knight.dirs)
      case Knight                         => shortRange(Knight.dirsOpposite)

      case Tokin if piece.color == Sente => shortRange(Tokin.dirs)
      case Tokin                         => shortRange(Tokin.dirsOpposite)

      case PromotedSilver if piece.color == Sente => shortRange(PromotedSilver.dirs)
      case PromotedSilver                         => shortRange(PromotedSilver.dirsOpposite)

      case PromotedLance if piece.color == Sente => shortRange(PromotedLance.dirs)
      case PromotedLance                         => shortRange(PromotedLance.dirsOpposite)

      case PromotedKnight if piece.color == Sente => shortRange(PromotedKnight.dirs)
      case PromotedKnight                         => shortRange(PromotedKnight.dirsOpposite)

      case Horse => longRange(Horse.dirs) ::: shortRange(King.dirs)

      case Dragon => longRange(Dragon.dirs) ::: shortRange(King.dirs)

      case King => shortRange(King.dirs)
    }
    def maybePromote(m: Move): Option[Move] =
      if (
        (Role.promotableRoles contains m.piece.role) &&
        ((m.color.promotableZone contains m.orig.y) || (m.color.promotableZone contains m.dest.y))
      ) {
        (m.after promote m.dest) map { b2 =>
          m.copy(after = b2, promotion = true)
        }
      } else None

    def forcePromotion(m: Move): Boolean = {
      m.piece.role match {
        case Pawn if m.piece.color.backrankY == m.dest.y && m.promotion == false  => false
        case Lance if m.piece.color.backrankY == m.dest.y && m.promotion == false => false
        case Knight
            if (m.piece.color.backrankY == m.dest.y ||
              m.piece.color.backrankY2 == m.dest.y) && m.promotion == false =>
          false
        case _ => true
      }
    }

    val promotedMoves = moves flatMap maybePromote

    (moves ++ promotedMoves).filter { m => forcePromotion(m) }
  }

  lazy val destinations: List[Pos] = moves map (_.dest)

  def color        = piece.color
  def is(c: Color) = c == piece.color
  def is(r: Role)  = r == piece.role
  def is(p: Piece) = p == piece

  /*
   *  Filters out moves that would put the king in check.
   *
   *  critical function. optimize for performance.
   */
  def kingSafetyMoveFilter(ms: List[Move]): List[Move] = {
    val filter: Piece => Boolean =
      if ((piece is King) || check) (_ => true) else (_.role.projection)
    val stableKingPos = if (piece is King) None else board kingPosOf color
    ms filter { m =>
      board.variant.kingSafety(m, filter, stableKingPos orElse (m.after kingPosOf color))
    }
  }

  lazy val check: Boolean = board check color

  private def shortRange(dirs: Directions): List[Move] =
    dirs flatMap { _(pos) } flatMap { to =>
      board.pieces.get(to) match {
        case None => board.move(pos, to) map { move(to, _) }
        case Some(piece) =>
          if (piece is color) Nil
          else board.taking(pos, to) map { move(to, _, Some(to)) }
      }
    }

  private def longRange(dirs: Directions): List[Move] = {
    val buf = new ArrayBuffer[Move]

    @tailrec
    def addAll(p: Pos, dir: Direction): Unit = {
      dir(p) match {
        case None => ()
        case s @ Some(to) =>
          board.pieces.get(to) match {
            case None => {
              board.move(pos, to).foreach { buf += move(to, _) }
              addAll(to, dir)
            }
            case Some(piece) =>
              if (piece.color != color) board.taking(pos, to) foreach {
                buf += move(to, _, s)
              }
          }
      }
    }

    dirs foreach { addAll(pos, _) }
    buf.toList
  }

  private def move(
      dest: Pos,
      after: Board,
      capture: Option[Pos] = None,
      promotion: Boolean = false
  ) =
    Move(
      piece = piece,
      orig = pos,
      dest = dest,
      situationBefore = Situation(board, piece.color),
      after = after,
      capture = capture,
      promotion = promotion
    )
}

object Actor {

  def longRangeThreatens(board: Board, p: Pos, dir: Direction, to: Pos): Boolean =
    board.variant.longRangeThreatens(board, p, dir, to)

}
