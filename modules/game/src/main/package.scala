package lila.game

import alleycats.Zero

export lila.core.lilaism.Lilaism.{ Game as CoreGame, Pov as CorePov, *, given }
export lila.common.extensions.*
export lila.core.id.{ GameFullId, GamePlayerId, GameAnyId }

private val logger = lila.log("game")

def rematchAlternatesColor(game: lila.core.game.Game, users: List[Option[lila.core.user.User]]): Boolean =
  !(game.fromPosition && users.count(_.exists(_.isBot)) == 1)
