package shogi
package variant

case object MiniShogi
    extends Variant(
      id = 2,
      key = "minishogi",
      name = "Mini Shogi",
      shortName = "MiniShogi",
      title = "Same rules, smaller board",
      standardInitialPosition = false
    ) {
  // this is just a placeholder for now
  def pieces = Standard.pieces

  override def opponentHasInsufficientMaterial(situation: Situation) = false
  override def isInsufficientMaterial(board: Board)                  = false
}
