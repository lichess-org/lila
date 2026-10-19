package lila.identity

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

private[identity] object BsonHandlers:

  given BSONHandler[IdentityStatus] = lila.db.dsl.quickHandler[IdentityStatus](
    { case BSONString(key) => IdentityStatus.fromKey(key) | IdentityStatus.Active },
    status => BSONString(status.key)
  )

  given BSONDocumentHandler[IdentityStatusRecord] = Macros.handler
  given BSONDocumentHandler[IdentityEvent]        = Macros.handler
