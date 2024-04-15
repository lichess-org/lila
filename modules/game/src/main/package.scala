package lila.game

import alleycats.Zero
import chess.ByColor

export lila.core.lilaism.Lilaism.{ Game as CoreGame, Pov as CorePov, *, given }
export lila.common.extensions.*
export lila.core.id.{ GameFullId, GamePlayerId, GameAnyId }

private val logger = lila.log("game")
