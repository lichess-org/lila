package shogi
package variant

import Pos._

case object Minishogi
    extends Variant(
      id = 2,
      key = "minishogi",
      name = "Minishogi",
      shortName = "Minishogi",
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
      Rook   -> 0,
      Bishop -> 0,
      Gold   -> 0,
      Silver -> 0,
      Pawn   -> 0
    )

  override val initialFen = "rbsgk/4p/5/P4/KGSBR b - 1"

  val allSquares = Pos.all5x5

  val numberOfRanks = 5
  val numberOfFiles = 5

  def promotionRanks(color: Color) = List(backrank(color))

  override def impasse(sit: Situation): Boolean = false

  override val allRoles = List(
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
