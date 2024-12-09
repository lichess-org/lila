package lila.event

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

private object BsonHandlers:

  given BSONDocumentHandler[Event] = Macros.handler
