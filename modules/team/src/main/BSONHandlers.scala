package lila.team

import reactivemongo.api.bson.Macros

import lila.hub.LeaderTeam

private object BSONHandlers {

  import lila.db.dsl.{ jodaDateTimeHandler, markdownHandler }
  given BSONDocumentHandler[Team] = Macros.handler
  given BSONDocumentHandler[Request] = Macros.handler
  given BSONDocumentHandler[TeamMember] = Macros.handler
  given BSONDocumentHandler[LeaderTeam] = Macros.handler
}
