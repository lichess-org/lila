package lila.insight

import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.db.AsyncColl

case class UserCache(
    _id: String, // user id
    count: Int, // nb insight entries
    ecos: Set[String],
    date: DateTime
) {

  def id = _id
}

private final class UserCacheApi(coll: AsyncColl) {

  private implicit val userCacheBSONHandler = Macros.handler[UserCache]

  def find(id: String) = coll(_.one[UserCache]($id(id)))

  def save(u: UserCache) = coll(_.update.one($id(u.id), u, upsert = true).void)

  def remove(id: String) = coll(_.delete.one($id(id)).void)
}
