package lila.game

import alleycats.Zero
import chess.ByColor

export lila.Lila.{ *, given }

type RatingDiffs = ByColor[IntRatingDiff]

private val logger = lila.log("game")
