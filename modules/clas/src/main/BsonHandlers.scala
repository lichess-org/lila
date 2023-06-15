package lila.clas

import lila.db.dsl.given
import reactivemongo.api.bson.*

private object BsonHandlers:

  given BSONDocumentHandler[Clas.Recorded] = Macros.handler
  given BSONDocumentHandler[Clas]          = Macros.handler
  given BSONDocumentHandler[Student]       = Macros.handler
  given BSONDocumentHandler[ClasInvite]    = Macros.handler
