package chess
package variant

case object Antichess
    extends Variant(
      id = 6,
      key = "antichess",
      name = "Antichess",
      shortName = "Anti",
      title = "Lose all your pieces (or get stalemated) to win the game.",
      standardInitialPosition = true
    ) {

  def pieces = Standard.pieces

  // In antichess, it is not permitted to castle
  override val castles    = Castles.none
  override val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"

  // In antichess, the king can't be put into check so we always return false
  override def kingThreatened(board: Board, color: Color, to: Pos, filter: Piece => Boolean = _ => true) =
    false

  // In this variant, a player must capture if a capturing move is available
  override def validMoves(situation: Situation) = {
    val allMoves       = super.validMoves(situation)
    val capturingMoves = allMoves.view mapValues (_.filter(_.captures)) filterNot (_._2.isEmpty)

    (if (!capturingMoves.isEmpty) capturingMoves else allMoves).to(Map)
  }

  override def valid(board: Board, strict: Boolean) =
    board.pieces.size >= 2 && board.pieces.size <= 40

  // In antichess, there is no checkmate condition, and the winner is the current player if they have no legal moves
  override def winner(situation: Situation): Option[Color] =
    if (specialEnd(situation)) Some(situation.color) else None

  override def specialEnd(situation: Situation) = {
    // The game ends with a win when one player manages to lose all their pieces or is in stalemate
    situation.board.piecesOf(situation.color).isEmpty || situation.moves.isEmpty
  }

  // In antichess, it is valuable for your opponent to have pieces.
  override def materialImbalance(board: Board): Int =
    board.pieces.values.foldLeft(0) {
      case (acc, Piece(color, _)) => acc + color.fold(-2, 2)
    }

  // In antichess, there is no checkmate condition therefore a player may only draw either by agreement,
  // blockade or stalemate. A player always has sufficient material to win otherwise.
  override def opponentHasInsufficientMaterial(situation: Situation) = false

  // No player can win if the only remaining pieces are opposing bishops on different coloured
  // diagonals. There may be pawns that are incapable of moving and do not attack the right color
  // of square to allow the player to force their opponent to capture their bishop, also resulting in a draw
  override def isInsufficientMaterial(board: Board) = {
    // Exit early if we are not in a situation with only bishops and pawns
    val bishopsAndPawns = board.pieces.values.forall(p => p.is(Bishop) || p.is(Pawn)) &&
      board.pieces.values.exists(_.is(Bishop))

    lazy val drawnBishops = board.actors.values.partition(_.is(White)) match {
      case (whitePieces, blackPieces) =>
        val whiteBishops    = whitePieces.filter(_.is(Bishop))
        val blackBishops    = blackPieces.filter(_.is(Bishop))
        lazy val whitePawns = whitePieces.filter(_.is(Pawn))
        lazy val blackPawns = blackPieces.filter(_.is(Pawn))

        // We consider the case where a player has two bishops on the same diagonal after promoting.
        if (
          whiteBishops.map(_.pos.color).to(Set).size != 1 ||
          blackBishops.map(_.pos.color).to(Set).size != 1
        ) false
        else {
          for {
            whiteSquareColor <- whiteBishops.headOption map (_.pos.color)
            blackSquareColor <- blackBishops.headOption map (_.pos.color)
          } yield {
            whiteSquareColor != blackSquareColor && whitePawns.forall(
              pawnNotAttackable(_, blackSquareColor, board)
            ) &&
            blackPawns.forall(pawnNotAttackable(_, whiteSquareColor, board))
          }
        } getOrElse false
    }

    bishopsAndPawns && drawnBishops
  }

  private def pawnNotAttackable(pawn: Actor, oppositeBishopColor: Color, board: Board) = {
    // The pawn cannot attack a bishop or be attacked by a bishop

    InsufficientMatingMaterial.pawnBlockedByPawn(pawn, board)
  }

  override val roles = List(Rook, Knight, Bishop, Pawn)

  override val promotableRoles: List[PromotableRole] = List(Rook, Bishop, Knight)
}
