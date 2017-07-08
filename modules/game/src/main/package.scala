package lila

package object game extends PackageObject with WithPlay {

  type PgnMoves = Vector[String]
  type RatingDiffs = chess.Color.Map[Int]

  private[game] def logger = lila.log("game")
}
