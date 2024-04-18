package lila.ui

object ReverseRouterConversions:
  given Conversion[GameId, String]   = _.value
  given Conversion[UserId, String]   = _.value
  given Conversion[UserName, String] = _.value
