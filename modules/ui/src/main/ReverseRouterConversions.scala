package lila.ui

import lila.core.id.*

object ReverseRouterConversions:
  given Conversion[GameId, String]     = _.value
  given Conversion[GameFullId, String] = _.value
  given Conversion[GameAnyId, String]  = _.value
  given Conversion[UserId, String]     = _.value
  given Conversion[UserName, String]   = _.value
