package lila.event

import reactivemongo.api.bson.*
import play.api.i18n.Lang

import lila.db.dsl.{ *, given }

private object BsonHandlers:

  private given BSONHandler[Lang] = stringAnyValHandler[Lang](_.code, Lang.apply)

  given BSONDocumentHandler[Event] = Macros.handler
