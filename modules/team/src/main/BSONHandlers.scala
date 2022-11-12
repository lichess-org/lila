package lila.team

import reactivemongo.api.bson.*

import lila.hub.LeaderTeam
import lila.db.dsl.given

private object BSONHandlers:

  given BSONDocumentHandler[Team]       = Macros.handler
  given BSONDocumentHandler[Request]    = Macros.handler
  given BSONDocumentHandler[TeamMember] = Macros.handler
  given BSONDocumentHandler[LeaderTeam] = Macros.handler
