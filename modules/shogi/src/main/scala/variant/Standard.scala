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

  val pieces: Map[Pos, Piece] = Variant.symmetricRank(backRank, backRank2)
}
