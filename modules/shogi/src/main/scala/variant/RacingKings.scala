package shogi
package variant

case object RacingKings
    extends Variant(
      id = 9,
      key = "racingKings",
      name = "Racing Kings",
      shortName = "Racing",
      title = "Race your King to the eighth rank to win.",
      standardInitialPosition = false
    ) {

  override def allowsCastling = false

  // Both sides start on the first two ranks:
  // krbnNBRK
  // qrbnNBRQ
  override val pieces: Map[Pos, Piece] = Map(
    Pos.A1 -> Gote.lance,
    Pos.A2 -> Gote.king,
    Pos.B1 -> Gote.rook,
    Pos.B2 -> Gote.rook,
    Pos.C1 -> Gote.bishop,
    Pos.C2 -> Gote.bishop,
    Pos.D1 -> Gote.knight,
    Pos.D2 -> Gote.knight,
    Pos.E1 -> Sente.knight,
    Pos.E2 -> Sente.knight,
    Pos.F1 -> Sente.bishop,
    Pos.F2 -> Sente.bishop,
    Pos.G1 -> Sente.rook,
    Pos.G2 -> Sente.rook,
    Pos.H1 -> Sente.lance,
    Pos.H2 -> Sente.king
  )

  override val castles = Castles.none

  override val initialFen = "8/8/8/8/8/8/krbnNBRK/qrbnNBRQ w - - 0 1"

  override def isInsufficientMaterial(board: Board)                  = false
  override def opponentHasInsufficientMaterial(situation: Situation) = false

  private def reachedGoal(board: Board, color: Color) =
    board.kingPosOf(color) exists (_.y == 8)

  private def reachesGoal(move: Move) =
    reachedGoal(move.situationAfter.board, move.piece.color)

  // It is a win, when exactly one king made it to the goal. When sente reaches
  // the goal and gote can make it on the next ply, he is given a chance to
  // draw, to compensate for the first-move advantage. The draw is not called
  // automatically, because gote should also be given equal chances to flag.
  override def specialEnd(situation: Situation) =
    situation.color match {
      case Sente =>
        reachedGoal(situation.board, Sente) ^ reachedGoal(situation.board, Gote)
      case Gote =>
        reachedGoal(situation.board, Sente) && (validMoves(situation).view mapValues (_.filter(reachesGoal)))
          .forall(_._2.isEmpty)
    }

  // If sente reaches the goal and gote also reaches the goal directly after,
  // then it is a draw.
  override def specialDraw(situation: Situation) =
    situation.color.sente && reachedGoal(situation.board, Sente) && reachedGoal(situation.board, Gote)

  override def winner(situation: Situation): Option[Color] =
    specialEnd(situation) option Color(reachedGoal(situation.board, Sente))

  // Not only check that our king is safe,
  // but also check the opponent's
  override def kingSafety(m: Move, filter: Piece => Boolean, kingPos: Option[Pos]): Boolean =
    super.kingSafety(m, filter, kingPos) && ! {
      m.after.kingPos get !m.color exists { theirKingPos =>
        kingThreatened(m.after, m.color, theirKingPos, (_ => true))
      }
    }

  // When considering stalemate, take into account that checks are not allowed.
  override def staleMate(situation: Situation): Boolean =
    !situation.check && !specialEnd(situation) && !validMoves(situation).exists(_._2.nonEmpty)
}
