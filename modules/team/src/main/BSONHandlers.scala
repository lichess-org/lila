package lila.team

import reactivemongo.api.bson.*

import lila.core.team.{ Access, LightTeam, TeamData }
import lila.db.dsl.{ *, given }

private object BSONHandlers:

  given BSONHandler[Access] = valueMapHandler(Access.byId)(_.id)
  given BSONDocumentHandler[Team] = Macros.handler
  given BSONDocumentHandler[TeamRequest] = Macros.handler
  given BSONHandler[TeamSecurity.Permission] = valueMapHandler(TeamSecurity.Permission.byKey)(_.key)
  given BSONDocumentHandler[TeamMember] =
    Macros.handler[TeamMember].afterWrite(lila.db.Util.removeEmptyArray("perms"))

  given BSONDocumentHandler[LightTeam] = Macros.handler
  given BSONDocumentHandler[TeamData] = Macros.handler
