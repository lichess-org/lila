package lila.pool

import reactivemongo.bson._

import lila.db.api._
import lila.db.Implicits._

private[pool] final class PoolRepo(coll: Coll) {

  def byId(id: ID) = coll.find(select(id)).one[Pool]

  def exists(id: ID) =
    coll.db command Count(coll.name, select(id).some) map (0 !=)

  private def select(id: ID) = BSONDocument("_id" -> id)
}
