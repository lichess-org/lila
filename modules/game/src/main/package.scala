package lidraughts

package object game extends PackageObject {

  type PdnMoves = Vector[String]
  type RatingDiffs = draughts.Color.Map[Int]

  private[game] def logger = lidraughts.log("game")
}
