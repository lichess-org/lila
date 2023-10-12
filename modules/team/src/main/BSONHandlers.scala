package lila.team

import reactivemongo.api.bson.*

import lila.hub.LightTeam
import lila.db.dsl.{ *, given }

private object BSONHandlers:

  given BSONDocumentHandler[Team]            = Macros.handler
  given BSONDocumentHandler[Request]         = Macros.handler
  given BSONHandler[TeamSecurity.Permission] = valueMapHandler(TeamSecurity.Permission.byKey)(_.key)
  given BSONDocumentHandler[TeamMember]      = Macros.handler
  given BSONDocumentHandler[LightTeam]      = Macros.handler
