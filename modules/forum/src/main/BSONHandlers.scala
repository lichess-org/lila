package lila.forum

private object BSONHandlers {

  import lila.db.BSON.BSONJodaDateTimeHandler
  implicit val CategBSONHandler = reactivemongo.bson.Macros.handler[Categ]
  implicit val TopicBSONHandler = reactivemongo.bson.Macros.handler[Topic]
  implicit val PostBSONHandler = reactivemongo.bson.Macros.handler[Post]
}
