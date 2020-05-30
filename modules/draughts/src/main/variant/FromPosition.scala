package draughts
package variant

case object FromPosition extends Variant(
  id = 3,
  gameType = 99,
  key = "fromPosition",
  name = "From Position",
  shortName = "FEN",
  title = "Custom starting position",
  standardInitialPosition = false,
  boardSize = Board.D100
) {

  def pieces = Standard.pieces
  def initialFen = Standard.initialFen
  def startingPosition = Standard.startingPosition

  def captureDirs = Standard.captureDirs
  def moveDirsColor = Standard.moveDirsColor
  def moveDirsAll = Standard.moveDirsAll
}
