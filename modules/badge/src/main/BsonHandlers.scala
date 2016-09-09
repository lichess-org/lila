package lila.badge

import lila.db.dsl._
import reactivemongo.bson._

private[badge] object BsonHandlers {

  implicit val BadgeBSONHandler = new BSONHandler[BSONString, Badge] {
    def read(x: BSONString) = Badge byId x.value err s"Invalid badge ${x.value}"
    def write(x: Badge) = BSONString(x.id)
  }

  implicit val UserBadgeBSONHandler = Macros.handler[UserBadge]
}
