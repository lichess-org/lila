package lila.forum

import lila.db.dsl._
import reactivemongo.api.bson._

private object BSONHandlers {

  implicit val CategBSONHandler: BSONDocumentHandler[Categ] = Macros.handler[Categ]

  implicit val PostEditBSONHandler: BSONDocumentHandler[OldVersion] = Macros.handler[OldVersion]
  implicit val PostBSONHandler: BSONDocumentHandler[Post]     = Macros.handler[Post]

  implicit val TopicBSONHandler: BSONDocumentHandler[Topic] = Macros.handler[Topic]
}
