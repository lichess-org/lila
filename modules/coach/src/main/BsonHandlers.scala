package lila.coach

import lila.db.dsl._
import reactivemongo.bson._

private[coach] object BsonHandlers {

  implicit val CoachIdBSONHandler = stringAnyValHandler[Coach.Id](_.value, Coach.Id.apply)

  implicit val CoachBSONHandler = Macros.handler[Coach]
}
