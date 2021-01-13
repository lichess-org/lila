package lila.insight

import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.db.AsyncColl
import lila.user.User

case class UserCache(
    _id: User.ID, // user id
    count: Int,   // nb insight entries
    ecos: Set[String],
    date: DateTime
) {

  def id = _id
}

object UserCache {

  def make(userId: User.ID, count: Int, ecos: Set[String]) =
    UserCache(userId, count, ecos, DateTime.now)
}

final private class UserCacheApi(coll: AsyncColl)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val userCacheBSONHandler = Macros.handler[UserCache]

  def find(id: String) = coll(_.one[UserCache]($id(id)))

  def save(u: UserCache) = coll(_.update.one($id(u.id), u, upsert = true).void)

  def remove(id: String) = coll(_.delete.one($id(id)).void)
}
