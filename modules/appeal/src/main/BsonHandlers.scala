package lila.appeal

import reactivemongo.api.bson._

import lila.db.dsl._

private[appeal] object BsonHandlers {

  import Appeal.Status

  implicit val statusHandler: BSONHandler[Status] = lila.db.dsl.quickHandler[Status](
    {
      case BSONString(v) => Status(v) | Status.Closed
      case _             => Status.Closed
    },
    s => BSONString(s.key),
  )

  implicit val appealMsgHandler: BSONDocumentHandler[AppealMsg] = Macros.handler[AppealMsg]
  implicit val appealHandler: BSONDocumentHandler[Appeal]       = Macros.handler[Appeal]
}
