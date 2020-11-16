package chess

/**
  * Utility methods for helping to determine whether a situation is a draw or a draw
  * on a player flagging.
  *
  * See http://www.e4ec.org/immr.html
  */
object InsufficientMatingMaterial {

  def nonKingPieces(board: Board) = board.pieces filter (_._2.role != King)

  def bishopsOnOppositeColors(board: Board) =
    (board.pieces collect { case (pos, Piece(_, Bishop)) => pos.color } toList).distinct.size == 2

  /*
   * Returns true if a pawn cannot progress forward because it is blocked by a pawn
   */
  def pawnBlockedByPawn(pawn: Actor, board: Board) = false

  /*
   * Determines whether a board position is an automatic draw due to neither player
   * being able to mate the other as informed by the traditional chess rules.
   */
  def apply(board: Board) = {
    lazy val kingsAndBishopsOnly = board.pieces forall { p =>
      (p._2 is King) || (p._2 is Bishop)
    }
    val kingsAndMinorsOnly = board.pieces forall { p =>
      (p._2 is King) || (p._2 is Bishop) || (p._2 is Knight)
    }

    kingsAndMinorsOnly && (board.pieces.size <= 3 || (kingsAndBishopsOnly && !bishopsOnOppositeColors(board)))
  }

  /*
   * Determines whether a color does not have mating material. In general:
   * King by itself is not mating material
   * King + knight mates against king + any(rook, bishop, knight, pawn)
   * King + bishop mates against king + any(bishop, knight, pawn)
   * King + bishop(s) versus king + bishop(s) depends upon bishop square colors
   */
  def apply(board: Board, color: Color) = {

    val kingsAndMinorsOnlyOfColor = board.piecesOf(color) forall { p =>
      (p._2 is King) || (p._2 is Bishop) || (p._2 is Knight)
    }
    lazy val nonKingRolesOfColor  = board rolesOf color filter (King !=)
    lazy val rolesOfOpponentColor = board rolesOf !color

    kingsAndMinorsOnlyOfColor && ((nonKingRolesOfColor toList).distinct match {
      case Nil => true
      case List(Knight) =>
        (nonKingRolesOfColor.size == 1) && (rolesOfOpponentColor filter (King !=) filter (Lance !=) isEmpty)
      case List(Bishop) =>
        !(rolesOfOpponentColor.exists(r => r == Knight || r == Pawn) || bishopsOnOppositeColors(board))
      case _ => false
    })
  }
}
