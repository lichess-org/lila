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
  import Variant._

  val pieces = symmetricFourRank(Vector(Man, Man, Man, Man, Man), boardSize)
  val initialFen = format.Forsyth.initial
  val startingPosition = StartingPosition("---", initialFen, "", "Initial position".some)
  val openings = Nil

  val captureDirs: Directions = List((UpLeft, _.moveUpLeft), (UpRight, _.moveUpRight), (DownLeft, _.moveDownLeft), (DownRight, _.moveDownRight))
  val moveDirsColor: Map[Color, Directions] = Map(White -> List((UpLeft, _.moveUpLeft), (UpRight, _.moveUpRight)), Black -> List((DownLeft, _.moveDownLeft), (DownRight, _.moveDownRight)))
  val moveDirsAll: Directions = moveDirsColor(White) ::: moveDirsColor(Black)
}
