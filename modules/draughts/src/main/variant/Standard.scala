package draughts
package variant

case object Standard extends Variant(
  id = 1,
  gameType = 20,
  key = "standard",
  name = "Standard",
  shortName = "Std",
  title = "Standard rules of international draughts",
  standardInitialPosition = true
) {

  val pieces: Map[Pos, Piece] = Variant.symmetricFourRank(standardRank)

}
