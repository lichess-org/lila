package shogi
package variant

import shogi.Pos._
import shogi.format.forsyth.Sfen

case object Minishogi
    extends Variant(
      id = 2,
      key = "minishogi",
      name = "Minishogi",
      shortName = "Minishogi",
      title = "Same rules, smaller board"
    ) {

  val initialSfen = Sfen("rbsgk/4p/5/P4/KGSBR b - 1")

  val numberOfRanks = 5
  val numberOfFiles = 5

  val allPositions = (SQ5E upTo SQ1A).toList

  val pieces =
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
  
  val hands = Map.empty

  val allRoles = List(
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

  val handRoles = List(
    Rook,
    Bishop,
    Gold,
    Silver,
    Pawn
  )

  def promote(role: Role) = Standard.promote(role)
  def unpromote(role: Role) = Standard.unpromote(role)

  def backrank(color: Color) =
    if (color.sente) Rank.A else Rank.E

  def promotionRanks(color: Color) = List(backrank(color))

  override def impasse(sit: Situation): Boolean = false

}
