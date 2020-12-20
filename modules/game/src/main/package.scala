package lila

package object game extends PackageObject {

  type PgnMoves    = Vector[String]
  type RatingDiffs = chess.Color.Map[Int]

  private[game] def logger = lila.log("game")
}
