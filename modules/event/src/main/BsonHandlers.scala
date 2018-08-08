package lidraughts.event

import reactivemongo.bson._
import play.api.i18n.Lang

import lidraughts.db.dsl._

private[event] object BsonHandlers {

  private implicit val UserIdBsonHandler = stringAnyValHandler[Event.UserId](_.value, Event.UserId.apply)

  private implicit val LangBsonHandler = stringAnyValHandler[Lang](_.code, Lang.apply)

  implicit val EventBsonHandler = Macros.handler[Event]
}
