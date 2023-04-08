package lila.forum

import lila.db.dsl.given
import reactivemongo.api.bson.*

private object BSONHandlers:

  given BSONDocumentHandler[ForumCateg] = Macros.handler

  given BSONDocumentHandler[OldVersion] = Macros.handler
  given BSONDocumentHandler[ForumPost]  = Macros.handler

  given BSONDocumentHandler[ForumTopic] = Macros.handler
