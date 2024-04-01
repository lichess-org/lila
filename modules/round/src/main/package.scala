package lila.round

import lila.game.Event

export lila.Core.{ *, given }

private val logger = lila.log("round")

private type Events = List[lila.core.game.Event]

enum OnTv:
  case Lichess(channel: String, flip: Boolean)
  case User(userId: UserId)
