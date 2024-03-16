package lila.team

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.hub.LightTeam

private object BSONHandlers:

  given BSONDocumentHandler[Team]            = Macros.handler
  given BSONDocumentHandler[TeamRequest]     = Macros.handler
  given BSONHandler[TeamSecurity.Permission] = valueMapHandler(TeamSecurity.Permission.byKey)(_.key)
  given BSONDocumentHandler[TeamMember] =
    Macros.handler[TeamMember].afterWrite(lila.db.Util.removeEmptyArray("perms"))

  given BSONDocumentHandler[LightTeam] = Macros.handler
