package lila.insight

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import reactivemongo.bson.Macros

import lila.db.BSON._
import lila.db.Implicits._

case class UserCache(
    _id: String, // user id
    count: Int, // nb insight entries
    ecos: Set[String],
    date: DateTime) {

  def id = _id
}

private final class UserCacheApi(coll: Coll) {

  private implicit val userCacheBSONHandler = Macros.handler[UserCache]

  def find(id: String) = coll.find(selectId(id)).one[UserCache]

  def save(u: UserCache) = coll.update(selectId(u.id), u, upsert = true).void

  def remove(id: String) = coll.remove(selectId(id)).void

  private def selectId(id: String) = BSONDocument("_id" -> id)
}
