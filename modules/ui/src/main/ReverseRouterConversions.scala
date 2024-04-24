package lila.ui

import lila.core.id.*

object ReverseRouterConversions:
  given Conversion[GameId, String]                   = _.value
  given Conversion[GameFullId, String]               = _.value
  given Conversion[GameAnyId, String]                = _.value
  given Conversion[UserId, String]                   = _.value
  given Conversion[UserName, String]                 = _.value
  given perfKeyConv: Conversion[PerfKey, String]     = _.value
  given Conversion[Option[UserName], Option[String]] = UserName.raw(_)
  // where a UserStr is accepted, we can pass a UserName or UserId
  given Conversion[UserName, UserStr] = _.into(UserStr)
  given Conversion[UserId, UserStr]   = _.into(UserStr)
