package lila.insight

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._

case class UserCache(
    _id: String, // user id
    count: Int, // nb insight entries
    ecos: Set[String],
    date: DateTime
) {

  def id = _id
}

private final class UserCacheApi(coll: Coll) {

  private implicit val userCacheBSONHandler = Macros.handler[UserCache]

  def find(id: String): Fu[Option[UserCache]] =
    coll.find($id(id)).one[UserCache]

  def save(u: UserCache): Funit =
    coll.update.one($id(u.id), u, upsert = true).void

  def remove(id: String): Funit = coll.delete.one($id(id)).void
}
