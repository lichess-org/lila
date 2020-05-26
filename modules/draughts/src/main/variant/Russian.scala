package draughts
package variant

case object Russian extends Variant(
  id = 11,
  gameType = 25,
  key = "russian",
  name = "Russian",
  shortName = "Russian",
  title = "Russian draughts",
  standardInitialPosition = false,
  boardSize = Board.D64
) {

  val pieces: Map[Pos, Piece] = Variant.symmetricThreeRank(Vector(Man, Man, Man, Man), boardSize)

  def captureDirs = Standard.captureDirs
  def moveDirsColor = Standard.moveDirsColor

  override val initialFen = "W:W21,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,12:H0:F1"

  override def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles = board rolesOf color
    (roles.count(_ == Man) > 0 || roles.count(_ == King) > 0) &&
      (!strict || roles.size <= 12) &&
      !menOnPromotionRank(board, color)
  }
}