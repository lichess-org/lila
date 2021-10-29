package shogi
package variant

import Pos._

case object MiniShogi
    extends Variant(
      id = 2,
      key = "minishogi",
      name = "Mini Shogi",
      shortName = "MiniShogi",
      title = "Same rules, smaller board",
      standardInitialPosition = false
    ) {

  def pieces = {
    val gotePieces = Map(
      SQ1A -> Gote.king,
      SQ2A -> Gote.gold,
      SQ3A -> Gote.silver,
      SQ4A -> Gote.bishop,
      SQ5A -> Gote.rook,
      SQ1B -> Gote.pawn
    )
    val sentePieces = Map(
      SQ5E -> Sente.king,
      SQ4E -> Sente.gold,
      SQ3E -> Sente.silver,
      SQ2E -> Sente.bishop,
      SQ1E -> Sente.rook,
      SQ5D -> Sente.pawn
    )
    gotePieces ++ sentePieces
  }

  override val initialFen = "rbsgk/4p/5/P4/KGSBR b - 1"

  override val numberOfRanks = 5
  override val numberOfFiles = 5

  private def isInsideBoard(pos: Pos): Boolean =
    Pos.all5x5.contains(pos)

  override def validMoves(situation: Situation): Map[Pos, List[Move]] =
    situation.actors
      .collect {
        case actor if actor.moves.nonEmpty => actor.pos -> actor.moves.filter(m => isInsideBoard(m.dest))
      }
      .to(Map)

  override def isValidPromotion(piece: Piece, promotion: Boolean, orig: Pos, dest: Pos) = {
    piece.role match {
      case Pawn if (if (piece.color == Sente) dest.y == 1 else dest.y == 5) && !promotion => false
      case _
          if !promotion || (promotion && ((if (piece.color == Sente) dest.y == 1 else dest.y == 5) || (if (
                                                                                                         piece.color == Sente
                                                                                                       ) dest.y == 1
                                                                                                       else
                                                                                                         dest.y == 5))) =>
        true
      case _ => false
    }
  }

  def backrank(color: Color) = if(color == Sente) 1 else 5

  override def canPromote(move: Move): Boolean =
    (Role.promotableRoles contains move.piece.role) &&
      ((backrank(move.color) == move.orig.y) || (backrank(move.color) == move.dest.y))

  override def mustPromote(move: Move): Boolean =
    !move.promotion && {
      move.piece.role match {
        case Pawn | Lance if backrank(move.piece.color) == move.dest.y => true
        case Knight if (backrank(move.piece.color) == move.dest.y ||
              backrank(move.piece.color) == move.dest.y) => true
        case _ => false
      }
    }

  override def allSquares = Pos.all5x5
}
