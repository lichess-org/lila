package lila.clas

import reactivemongo.api.bson.*

import lila.db.dsl.given

private object BsonHandlers:

  given BSONDocumentHandler[Clas.Recorded] = Macros.handler
  given BSONDocumentHandler[Clas] = Macros.handler
  given BSONDocumentHandler[Student] = Macros.handler
  given BSONDocumentHandler[ClasInvite] = Macros.handler
