package lila.user

import lila.common.LightUser

import lila.db.dsl._
import reactivemongo.bson._
import scala.concurrent.duration._
import User.{ BSONFields => F }

final class LightUserApi(coll: Coll) {

  def get(id: String): Option[LightUser] = cache get id

  def invalidate = cache invalidate _

  private implicit val lightUserReader = new BSONDocumentReader[LightUser] {

    def read(doc: BSONDocument) = LightUser(
      id = doc.getAs[String](F.id) err "LightUser id missing",
      name = doc.getAs[String](F.username) err "LightUser username missing",
      title = doc.getAs[String](F.title))
  }

  private val cache = lila.memo.MixedCache[String, Option[LightUser]](
    id => coll.find(
      BSONDocument(F.id -> id),
      BSONDocument(F.username -> true, F.title -> true)
    ).one[LightUser],
    timeToLive = 20 minutes,
    default = id => LightUser(id, id, None).some,
    logger = logger branch "LightUserApi")
}
