package lila.event

import reactivemongo.api.bson._
import play.api.i18n.Lang

import lila.db.dsl._

private[event] object BsonHandlers {

  implicit private val UserIdBsonHandler = stringAnyValHandler[Event.UserId](_.value, Event.UserId.apply)

  implicit private val LangBsonHandler = stringAnyValHandler[Lang](_.code, Lang.apply)

  implicit val EventBsonHandler = Macros.handler[Event]
}
