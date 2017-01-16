package lila.user

import lila.common.LightUser

import lila.db.dsl._
import reactivemongo.bson._
import scala.concurrent.duration._
import User.{ BSONFields => F }

final class LightUserApi(coll: Coll) {

  def get(id: String): Option[LightUser] = cache get id

  def invalidate = cache invalidate _

  def getList(ids: List[String]): List[LightUser] = ids flatMap get

  def usernameList(ids: List[String]): List[String] = getList(ids).map(_.name)

  private implicit val lightUserReader = new BSONDocumentReader[LightUser] {

    def read(doc: BSONDocument) =
      LightUser(
        id = doc.getAs[String](F.id) err "LightUser id missing",
        name = doc.getAs[String](F.username) err "LightUser username missing",
        title = doc.getAs[String](F.title),
        isPatron = ~doc.getAs[Bdoc](F.plan).flatMap(_.getAs[Boolean]("active")))
  }

  private val cache = lila.memo.MixedCache[String, Option[LightUser]](
    name = "user.light",
    id => coll.find(
      $id(id),
      $doc(F.username -> true, F.title -> true, s"${F.plan}.active" -> true)
    ).uno[LightUser],
    timeToLive = 20 minutes,
    default = id => LightUser(id, id, None, false).some,
    logger = logger branch "LightUserApi")
}
