package lila.event

import reactivemongo.api.bson.*
import play.api.i18n.Lang

import lila.db.dsl.{ *, given }

private object BsonHandlers:

  private given BSONHandler[Lang] = langByCodeHandler

  given BSONDocumentHandler[Event] = Macros.handler
