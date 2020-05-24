package draughts
package variant

case object Russian extends Variant(
  id = 1,
  gameType = 25,
  key = "russian",
  name = "Russian",
  shortName = "Russian",
  title = "Russian draughts",
  standardInitialPosition = true,
  boardSize = Board.D64
) {

  val pieces: Map[Pos, Piece] = Variant.symmetricThreeRank(Vector(Man, Man, Man, Man))

}