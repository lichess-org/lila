package lila.team

import reactivemongo.api.bson.Macros

import lila.hub.LeaderTeam

private object BSONHandlers {

  import lila.db.dsl.{ jodaDateTimeHandler, markdownHandler }
  implicit val TeamBSONHandler       = Macros.handler[Team]
  implicit val RequestBSONHandler    = Macros.handler[Request]
  implicit val MemberBSONHandler     = Macros.handler[TeamMember]
  implicit val LeaderTeamBSONHandler = Macros.handler[LeaderTeam]
}
