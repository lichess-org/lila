package lila.ui

object ReverseRouterConversions:
  given Conversion[GameId, String] = _.value
