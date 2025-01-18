package lila.forum

import reactivemongo.api.bson._

import lila.db.dsl._

private object BSONHandlers {

  implicit val CategBSONHandler: BSONDocumentHandler[Categ] = Macros.handler[Categ]

  implicit val PostEditBSONHandler: BSONDocumentHandler[OldVersion] = Macros.handler[OldVersion]
  implicit val PostBSONHandler: BSONDocumentHandler[Post]     = Macros.handler[Post]

  implicit val TopicBSONHandler: BSONDocumentHandler[Topic] = Macros.handler[Topic]
}
