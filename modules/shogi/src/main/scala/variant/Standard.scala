package shogi
package variant

case object Standard
    extends Variant(
      id = 1,
      key = "standard",
      name = "Standard",
      shortName = "Std",
      title = "Standard rules of shogi",
      standardInitialPosition = true
    ) {

  val pieces: Map[Pos, Piece] = Variant.defaultPieces
  val hand: Map[Role, Int]    = Variant.defaultHand

  val numberOfRanks: Int = 9
  val numberOfFiles: Int = 9

  val allSquares = Pos.all9x9

  def promotionRanks(color: Color) = if (color == Sente) List(1, 2, 3) else List(7, 8, 9)
}
