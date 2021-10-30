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

  def pieces = 
    Map(
      SQ5E -> Sente.king,
      SQ4E -> Sente.gold,
      SQ3E -> Sente.silver,
      SQ2E -> Sente.bishop,
      SQ1E -> Sente.rook,
      SQ5D -> Sente.pawn,
      SQ1A -> Gote.king,
      SQ2A -> Gote.gold,
      SQ3A -> Gote.silver,
      SQ4A -> Gote.bishop,
      SQ5A -> Gote.rook,
      SQ1B -> Gote.pawn
    )

  def hand =
    Map(
      Rook -> 0,
      Bishop -> 0,
      Gold -> 0,
      Silver -> 0,
      Pawn -> 0
    )

  override val initialFen = "rbsgk/4p/5/P4/KGSBR b - 1"

  override def allSquares = Pos.all5x5

  override val numberOfRanks = 5
  override val numberOfFiles = 5

  override def backrank(color: Color) = if(color == Sente) 1 else 5

  override def promotionZone(color: Color) = List(backrank(color))

  private def isInsideBoard(pos: Pos): Boolean =
    Pos.all5x5.contains(pos)

  override def pieceInDeadZone(piece: Piece, pos: Pos): Boolean =
    piece.role match {
      case Pawn if backrank(piece.color) == pos.y => true
      case _ => false
    }

  override def validMoves(situation: Situation): Map[Pos, List[Move]] =
    situation.actors
      .collect {
        case actor if actor.moves.nonEmpty => actor.pos -> actor.moves.filter(m => isInsideBoard(m.dest))
      }
      .to(Map)

  override def isValidPromotion(piece: Piece, promotion: Boolean, orig: Pos, dest: Pos) = {
    piece.role match {
      case Pawn if (!promotion && backrank(piece.color) == dest.y) => false
      case _ if !promotion || (promotion && (backrank(piece.color) == dest.y || backrank(piece.color) == orig.y)) =>
        true
      case _ => false
    }
  }

  override def canPromote(move: Move): Boolean =
    (promotableRoles contains move.piece.role) &&
      ((backrank(move.color) == move.orig.y) || (backrank(move.color) == move.dest.y))

  override val roles = List(
    Pawn,
    Silver,
    Gold,
    King,
    Bishop,
    Rook,
    PromotedSilver,
    Dragon,
    Horse,
    Tokin
  )

  override val handRoles: List[Role] = List(
    Rook,
    Bishop,
    Gold,
    Silver,
    Pawn
  )

}
