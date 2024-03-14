package lila.event

import play.api.i18n.Lang
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

private object BsonHandlers:

  private given BSONHandler[Lang] = langByCodeHandler

  given BSONDocumentHandler[Event] = Macros.handler
