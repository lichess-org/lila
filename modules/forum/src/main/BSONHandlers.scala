package lila.forum

import lila.db.BSON.LoggingHandler
import lila.db.dsl._
import reactivemongo.bson._

private object BSONHandlers {

  implicit val CategBSONHandler = Macros.handler[Categ]
  implicit val PostBSONHandler = Macros.handler[Post]
  private val topicHandler: BSONDocumentReader[Topic] with BSONDocumentWriter[Topic] with BSONHandler[Bdoc, Topic] = Macros.handler[Topic]
  implicit val TopicBSONHandler: BSONDocumentReader[Topic] with BSONDocumentWriter[Topic] with BSONHandler[Bdoc, Topic] = LoggingHandler(logger)(topicHandler)
}
