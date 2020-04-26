package lila.team

import reactivemongo.api.bson.Macros

import lila.hub.LightTeam

private object BSONHandlers {

  import lila.db.dsl.BSONJodaDateTimeHandler
  implicit val TeamBSONHandler      = Macros.handler[Team]
  implicit val RequestBSONHandler   = Macros.handler[Request]
  implicit val MemberBSONHandler    = Macros.handler[Member]
  implicit val LightTeamBSONHandler = Macros.handler[LightTeam]
}
