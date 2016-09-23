package lila.event

import lila.db.dsl._
import reactivemongo.bson._

private[event] object BsonHandlers {

  implicit val UserIdBsonHandler = stringAnyValHandler[Event.UserId](_.value, Event.UserId.apply)

  implicit val EventBsonHandler = Macros.handler[Event]
}
