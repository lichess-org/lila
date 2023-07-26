package lila.game

import alleycats.Zero
import chess.{ ByColor, PositionHash }

export lila.Lila.{ *, given }

type RatingDiffs = ByColor[IntRatingDiff]

private val logger = lila.log("game")
