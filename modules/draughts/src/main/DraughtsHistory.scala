package draughts

import format.Uci
import variant.{ Variant, Standard }

// Consecutive king moves by the respective side.
case class KingMoves(white: Int = 0, black: Int = 0, whiteKing: Option[Pos] = None, blackKing: Option[Pos] = None) {

  def add(color: Color, pos: Option[Pos]) = copy(
    white = white + color.fold(1, 0),
    black = black + color.fold(0, 1),
    whiteKing = color.fold(pos, whiteKing),
    blackKing = color.fold(blackKing, pos)
  )

  def reset(color: Color) = copy(
    white = color.fold(0, white),
    black = color.fold(black, 0),
    whiteKing = color.fold(none, whiteKing),
    blackKing = color.fold(blackKing, none)
  )

  def nonEmpty = white > 0 || black > 0

  def apply(color: Color) = color.fold(white, black)
  def kingPos(color: Color) = color.fold(whiteKing, blackKing)
}

case class DraughtsHistory(
    lastMove: Option[Uci] = None,
    positionHashes: PositionHash = Hash.zero,
    kingMoves: KingMoves = KingMoves(),
    variant: Variant = Standard
) {

  /**
   * Halfmove clock: This is the number of halfmoves
   * since the last non-king move or capture.
   * This is used to determine if a draw
   * can be claimed under the twentyfive-move rule.
   */
  def halfMoveClock = math.max(0, (positionHashes.length / Hash.size) - 1)

  // generates random positionHashes to satisfy the half move clock
  def setHalfMoveClock(v: Int) =
    copy(positionHashes = DraughtsHistory.spoofHashes(v + 1))

  /**
   * Checks for threefold repetition, does not apply to frisian draughts
   */
  def threefoldRepetition: Boolean = !variant.frisianVariant && halfMoveClock >= 8 && {
    // compare only hashes for positions with the same side to move
    val positions = (positionHashes grouped Hash.size).sliding(1, 2).flatten.toList
    positions.headOption match {
      case Some(Array(x, y, z)) => (positions count {
        case Array(x2, y2, z2) => x == x2 && y == y2 && z == z2
        case _ => false
      }) >= 3
      case _ => false
    }
  }

  def withLastMove(m: Uci) = copy(lastMove = Some(m))

  def withKingMove(color: Color, pos: Option[Pos], v: Boolean, resetOther: Boolean = false) =
    if (v && resetOther)
      copy(kingMoves = kingMoves.add(color, pos).reset(!color))
    else if (v)
      copy(kingMoves = kingMoves.add(color, pos))
    else if (resetOther)
      copy(kingMoves = KingMoves())
    else
      copy(kingMoves = kingMoves reset color)

  def withKingMoves(km: KingMoves) = copy(kingMoves = km)

  override def toString = {
    val positions = (positionHashes grouped Hash.size).toList
    s"${lastMove.fold("-")(_.uci)} ${positions.map(Hash.debug).mkString(" ")}"
  }
}

object DraughtsHistory {

  def make(
    lastMove: Option[String], // 0510 etc
    variant: Variant
  ): DraughtsHistory = DraughtsHistory(
    lastMove = lastMove flatMap Uci.apply,
    positionHashes = Array(),
    variant = variant
  )

  private def spoofHashes(n: Int): PositionHash = {
    (1 to n).toArray.flatMap {
      i => Array((i >> 16).toByte, (i >> 8).toByte, i.toByte)
    }
  }
}
