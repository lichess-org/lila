package lila.appeal

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

private object BsonHandlers:

  import Appeal.Status

  given BSONHandler[Status] = stringAnyValHandler(_.toString, t => Status(t) | Status.read)

  given BSONHandler[AppealTopic] =
    stringAnyValHandler(_.key, t => AppealTopic.byKey.getOrElse(t, AppealTopic.legacy))

  given BSONDocumentHandler[AppealMsg] = Macros.handler
  given BSONDocumentHandler[Appeal] = Macros.handler
