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
      title = doc.getAs[String](F.title),
      patron = doc.getAs[BSONDocument](F.plan).flatMap { plan =>
        plan.getAs[Int]("months")
      })
  }

  private val cache = lila.memo.MixedCache[String, Option[LightUser]](
    id => coll.find(
      $id(id),
      $doc(F.username -> true, F.title -> true, s"${F.plan}.months" -> true)
    ).uno[LightUser],
    timeToLive = 20 minutes,
    default = id => LightUser(id, id, None, None).some,
    logger = logger branch "LightUserApi")
}
