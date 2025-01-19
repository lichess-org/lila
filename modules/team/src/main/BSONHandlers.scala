package lila.team

import reactivemongo.api.bson.BSONDocumentHandler
import reactivemongo.api.bson.Macros

import lila.hub.LightTeam

private object BSONHandlers {

  import lila.db.dsl.BSONJodaDateTimeHandler
  implicit val TeamBSONHandler: BSONDocumentHandler[Team]           = Macros.handler[Team]
  implicit val RequestBSONHandler: BSONDocumentHandler[Request]     = Macros.handler[Request]
  implicit val MemberBSONHandler: BSONDocumentHandler[Member]       = Macros.handler[Member]
  implicit val LightTeamBSONHandler: BSONDocumentHandler[LightTeam] = Macros.handler[LightTeam]
}
