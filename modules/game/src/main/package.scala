package lila.game

import alleycats.Zero
import chess.ByColor

export lila.Core.{ *, given }

type RatingDiffs = ByColor[IntRatingDiff]

private val logger = lila.log("game")
