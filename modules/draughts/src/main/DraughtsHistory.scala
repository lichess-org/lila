package draughts

import format.Uci
import variant.{ Variant, Standard }

// Consecutive king moves by the respective side.
case class KingMoves(white: Int = 0, black: Int = 0) {

  def add(color: Color) = copy(
    white = white + color.fold(1, 0),
    black = black + color.fold(0, 1)
  )

  def reset(color: Color) = copy(
    white = color.fold(0, white),
    black = color.fold(black, 0)
  )

  def nonEmpty = white > 0 || black > 0

  def apply(color: Color) = color.fold(white, black)
}

case class DraughtsHistory(
    lastMove: Option[Uci] = None,
    positionHashes: PositionHash = Hash.zero,
    kingMoves: KingMoves = KingMoves(0, 0),
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
   * Checks for threefold repetition, does not apply to frisian chess or antidraughts
   */
  def threefoldRepetition: Boolean = !variant.frisian && !variant.antidraughts && halfMoveClock >= 8 && {
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

  def withKingMove(color: Color, v: Boolean, resetOther: Boolean = false) =
    if (v && resetOther)
      copy(kingMoves = kingMoves add color reset !color)
    else if (v)
      copy(kingMoves = kingMoves add color)
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
