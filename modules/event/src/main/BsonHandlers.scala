package lila.event

import play.api.i18n.Lang

import reactivemongo.api.bson._

import lila.db.dsl._

private[event] object BsonHandlers {

  implicit private val UserIdBsonHandler: BSONHandler[Event.UserId] = stringAnyValHandler[Event.UserId](_.value, Event.UserId.apply)

  implicit private val LangBsonHandler: BSONHandler[Lang] = stringAnyValHandler[Lang](_.code, Lang.apply)

  implicit val EventBsonHandler: BSONDocumentHandler[Event] = Macros.handler[Event]
}
