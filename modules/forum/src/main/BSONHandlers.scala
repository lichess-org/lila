package lila.forum

import lila.db.BSON.LoggingHandler
import lila.db.dsl._
import reactivemongo.bson._

private object BSONHandlers {

  implicit val CategBSONHandler = LoggingHandler(logger)(Macros.handler[Categ])

  implicit val PostEditBSONHandler = Macros.handler[OldVersion]
  implicit val PostBSONHandler = Macros.handler[Post]

  implicit val TopicBSONHandler = LoggingHandler(logger)(Macros.handler[Topic])
}
