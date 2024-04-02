package lila.game

import alleycats.Zero
import chess.ByColor

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

type RatingDiffs = ByColor[IntRatingDiff]

private val logger = lila.log("game")
