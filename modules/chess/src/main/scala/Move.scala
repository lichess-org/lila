package chess

import format.Uci

case class Move(
    piece: Piece,
    orig: Pos,
    dest: Pos,
    situationBefore: Situation,
    after: Board,
    capture: Option[Pos],
    promotion: Boolean,
    castle: Option[((Pos, Pos), (Pos, Pos))],
    enpassant: Boolean,
    metrics: MoveMetrics = MoveMetrics()
) {
  def before = situationBefore.board

  def situationAfter = Situation(finalizeAfter, !piece.color)

  def withHistory(h: History) = copy(after = after withHistory h)

  def finalizeAfter: Board = {
    val board = after updateHistory { h1 =>
      val h2 = h1.copy(
        lastMove = Some(toUci),
        unmovedRooks = before.unmovedRooks,
        halfMoveClock =
          if ((piece is Pawn) || captures || promotes) 0
          else h1.halfMoveClock + 1
      )

      // my broken castles
      if ((piece is King) && h2.canCastle(color).any)
        h2 withoutCastles color
      else if (piece is Rook) (for {
        kingPos <- after kingPosOf color
        side <- Side.kingRookSide(kingPos, orig).filter { s =>
          (h2 canCastle color on s) &&
          h1.unmovedRooks.pos(orig)
        }
      } yield h2.withoutCastle(color, side)) | h2
      else h2
    } fixCastles

    board.variant.finalizeBoard(board, toUci, capture flatMap before.apply, !situationBefore.color) updateHistory { h =>
      // Update position hashes last, only after updating the board,
      // castling rights and en-passant rights.
      h.copy(positionHashes = Hash(Situation(board, !piece.color)) ++ {
        if (board.variant.isIrreversible(this)) Array.empty: PositionHash
        else h.positionHashes
      })
    }
  }

  def applyVariantEffect: Move = before.variant addVariantEffect this

  def afterWithLastMove =
    after.variant.finalizeBoard(
      after.copy(history = after.history.withLastMove(toUci)),
      toUci,
      capture flatMap before.apply,
      !situationBefore.color
    )

  // does this move capture an opponent piece?
  def captures = capture.isDefined

  def promotes = promotion

  def castles = castle.isDefined

  def normalizeCastle =
    castle.fold(this) {
      case (_, (rookOrig, _)) => copy(dest = rookOrig)
    }

  def color = piece.color

  def withPromotion(op: Option[PromotableRole], promoting: Boolean): Option[Move] =
    if(!promoting)
      this.some
    else {
      op.fold(this.some) { p =>
        for {
          b2 <- after take dest
          b3 <- b2.place(color - p, dest)
        } yield copy(after = b3, promotion = true)
      }
    }

  def withAfter(newBoard: Board) = copy(after = newBoard)

  def withMetrics(m: MoveMetrics) = copy(metrics = m)

  def toUci = {
    Uci.Move(orig, dest, promotion)
  }

  override def toString = s"$piece ${toUci.uci}"
}
