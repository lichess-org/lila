package lila.appeal

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

private[appeal] object BsonHandlers:

  import Appeal.Status

  given BSONHandler[Status] = lila.db.dsl.quickHandler[Status](
    {
      case BSONString(v) => Status(v) | Status.Read
      case _             => Status.Read
    },
    s => BSONString(s.key)
  )

  given BSONDocumentHandler[AppealMsg] = Macros.handler
  given BSONDocumentHandler[Appeal]    = Macros.handler
