package lila.forum

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.*

private object BSONHandlers:

  given BSONDocumentHandler[Categ] = Macros.handler

  given BSONDocumentHandler[OldVersion] = Macros.handler
  given BSONDocumentHandler[Post] = Macros.handler

  given BSONDocumentHandler[Topic] = Macros.handler
