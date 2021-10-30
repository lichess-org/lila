package shogi

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

final case class Actor(
    piece: Piece,
    pos: Pos,
    board: Board
) {

  lazy val moves: List[Move] = kingSafetyMoveFilter(trustedMoves)

  // The moves without taking defending the king into account
  def trustedMoves: List[Move] = {
    val moves = shortRange(piece.shortRangeDirs) ::: longRange(piece.longRangeDirs)

    def promotions(m: Move): Option[Move] = {
      if (board.variant.canPromote(m))
        (m.after promote m.dest) map { b2 =>
          m.copy(after = b2, promotion = true)
        }
      else None
    }

    val movesWithPromotions =
      (moves ::: (moves flatMap promotions))
        .filterNot { m => !m.promotion && board.variant.pieceInDeadZone(piece, m.dest) }

    if (board.variant.hasMoveEffects) movesWithPromotions map (_.applyVariantEffect) else movesWithPromotions
  }

  lazy val destinations: List[Pos] = moves map (_.dest)

  def color        = piece.color
  def is(c: Color) = c == piece.color
  def is(r: Role)  = r == piece.role
  def is(p: Piece) = p == piece

  // Filters out moves that would put the king in check.
  // Critical function. Optimize for performance.
  def kingSafetyMoveFilter(ms: List[Move]): List[Move] = {
    val filter: Piece => Boolean =
      if ((piece is King) || check) (_ => true) else (_.longRangeDirs.nonEmpty)
    val stableKingPos = if (piece is King) None else board kingPosOf color
    ms filter { m =>
      board.variant.kingSafety(m, filter, stableKingPos orElse (m.after kingPosOf color))
    }
  }

  lazy val check: Boolean = board check color

  private def isInsideBoard(pos: Pos): Boolean =
    pos.x <= board.variant.numberOfFiles && pos.y <= board.variant.numberOfRanks

  private def shortRange(dirs: Directions): List[Move] =
    dirs flatMap { _(pos) } flatMap {
      case to if isInsideBoard(to) =>
        board.pieces.get(to) match {
          case None => board.move(pos, to) map { move(to, _) }
          case Some(piece) =>
            if (piece is color) None
            else board.taking(pos, to) map { move(to, _, Option(to)) }
        }
      case _ => None
    }

  private def longRange(dirs: Directions): List[Move] = {
    val buf = new ArrayBuffer[Move]

    @tailrec
    def addAll(p: Pos, dir: Direction): Unit = {
      dir(p) match {
        case s @ Some(to) if isInsideBoard(to) =>
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
        case _ => ()
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
