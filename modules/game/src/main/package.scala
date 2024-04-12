package lila.game

import alleycats.Zero
import chess.ByColor

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.id.{ GameFullId, GamePlayerId, GameAnyId }

type RatingDiffs = ByColor[IntRatingDiff]

private val logger = lila.log("game")
