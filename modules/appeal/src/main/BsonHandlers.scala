package lila.appeal

import lila.db.dsl._
import reactivemongo.api.bson._

private[appeal] object BsonHandlers {

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
}
