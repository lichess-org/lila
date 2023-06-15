package lila.coach

import reactivemongo.api.bson.*

import lila.db.dsl.given

private object BsonHandlers:

  given BSONDocumentHandler[CoachProfile] = Macros.handler
  given BSONDocumentHandler[Coach.User]   = Macros.handler
  given BSONDocumentHandler[Coach]        = Macros.handler
