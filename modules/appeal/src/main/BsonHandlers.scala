package lila.appeal

import lila.db.dsl._
import reactivemongo.api.bson._

private[appeal] object BsonHandlers {

  import Appeal.Status

  implicit val statusHandler = lila.db.dsl.quickHandler[Status](
    {
      case BSONString(v) => Status(v) | Status.Read
      case _             => Status.Read
    },
    s => BSONString(s.key)
  )

  implicit val appealMsgHandler = Macros.handler[AppealMsg]
  implicit val appealHandler    = Macros.handler[Appeal]
}
