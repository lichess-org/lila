package lila.event

import reactivemongo.api.bson._
import play.api.i18n.Lang

import lila.db.dsl._

private object BsonHandlers {

  private given BSONHandler[Event.UserId] = stringAnyValHandler[Event.UserId](_.value, Event.UserId.apply)

  private given BSONHandler[Lang] = stringAnyValHandler[Lang](_.code, Lang.apply)

  given BSONDocumentHandler[Event] = Macros.handler
}
