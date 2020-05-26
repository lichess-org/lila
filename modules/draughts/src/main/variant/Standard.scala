package draughts
package variant

case object Standard extends Variant(
  id = 1,
  gameType = 20,
  key = "standard",
  name = "Standard",
  shortName = "Std",
  title = "Standard rules of international draughts (FMJD)",
  standardInitialPosition = true,
  boardSize = Board.D100
) {

  val pieces: Map[Pos, Piece] = Variant.symmetricFourRank(Vector(Man, Man, Man, Man, Man), boardSize)
  val captureDirs: Directions = List((Variant.UpLeft, _.moveUpLeft), (Variant.UpRight, _.moveUpRight), (Variant.DownLeft, _.moveDownLeft), (Variant.DownRight, _.moveDownRight))
  val moveDirsColor: Map[Color, Directions] = Map(White -> List((Variant.UpLeft, _.moveUpLeft), (Variant.UpRight, _.moveUpRight)), Black -> List((Variant.DownLeft, _.moveDownLeft), (Variant.DownRight, _.moveDownRight)))
}
