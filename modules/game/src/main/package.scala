package lila.game

import alleycats.Zero
import cats.Monoid
import chess.{ ByColor, PositionHash }

export lila.Lila.{ *, given }

type RatingDiffs = ByColor[IntRatingDiff]

given Zero[PositionHash] = Zero(Monoid[PositionHash].empty)

private val logger = lila.log("game")
