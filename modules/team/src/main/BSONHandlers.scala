package lila.team

import lila.hub.lightTeam.LightTeam

private object BSONHandlers {

  import lila.db.dsl.BSONJodaDateTimeHandler
  implicit val TeamBSONHandler = reactivemongo.api.bson.Macros.handler[Team]
  implicit val RequestBSONHandler = reactivemongo.api.bson.Macros.handler[Request]
  implicit val MemberBSONHandler = reactivemongo.api.bson.Macros.handler[Member]
  implicit val LightTeamBSONHandler = reactivemongo.api.bson.Macros.handler[LightTeam]
}
