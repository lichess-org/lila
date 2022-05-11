package lila.poll

import lila.db.dsl._
import reactivemongo.api.bson._

private object BSONHandlers {

  implicit val PollBSONHandler = Macros.handler[Poll]
}
