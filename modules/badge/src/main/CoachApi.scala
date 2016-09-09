package lila.badge

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class BadgeApi(
    badgeColl: Coll) {

  import BsonHandlers._
}
