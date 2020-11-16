package chess
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
    Pos.A1 -> Black.lance,
    Pos.A2 -> Black.king,
    Pos.B1 -> Black.rook,
    Pos.B2 -> Black.rook,
    Pos.C1 -> Black.bishop,
    Pos.C2 -> Black.bishop,
    Pos.D1 -> Black.knight,
    Pos.D2 -> Black.knight,
    Pos.E1 -> White.knight,
    Pos.E2 -> White.knight,
    Pos.F1 -> White.bishop,
    Pos.F2 -> White.bishop,
    Pos.G1 -> White.rook,
    Pos.G2 -> White.rook,
    Pos.H1 -> White.lance,
    Pos.H2 -> White.king
  )

  override val castles = Castles.none

  override val initialFen = "8/8/8/8/8/8/krbnNBRK/qrbnNBRQ w - - 0 1"

  override def isInsufficientMaterial(board: Board)                  = false
  override def opponentHasInsufficientMaterial(situation: Situation) = false

  private def reachedGoal(board: Board, color: Color) =
    board.kingPosOf(color) exists (_.y == 8)

  private def reachesGoal(move: Move) =
    reachedGoal(move.situationAfter.board, move.piece.color)

  // It is a win, when exactly one king made it to the goal. When white reaches
  // the goal and black can make it on the next ply, he is given a chance to
  // draw, to compensate for the first-move advantage. The draw is not called
  // automatically, because black should also be given equal chances to flag.
  override def specialEnd(situation: Situation) =
    situation.color match {
      case White =>
        reachedGoal(situation.board, White) ^ reachedGoal(situation.board, Black)
      case Black =>
        reachedGoal(situation.board, White) && (validMoves(situation).view mapValues (_.filter(reachesGoal)))
          .forall(_._2.isEmpty)
    }

  // If white reaches the goal and black also reaches the goal directly after,
  // then it is a draw.
  override def specialDraw(situation: Situation) =
    situation.color.white && reachedGoal(situation.board, White) && reachedGoal(situation.board, Black)

  override def winner(situation: Situation): Option[Color] =
    specialEnd(situation) option Color(reachedGoal(situation.board, White))

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
