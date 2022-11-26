package lila.game

export lila.Lila.{ *, given }

type PgnMoves    = Vector[String]
type RatingDiffs = chess.Color.Map[IntRatingDiff]

private val logger = lila.log("game")
