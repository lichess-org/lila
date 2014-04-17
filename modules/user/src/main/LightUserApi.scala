package lila.user

import lila.common.LightUser

import lila.db.Types._
import reactivemongo.bson._
import scala.concurrent.duration._
import User.BSONFields._

final class LightUserApi(coll: Coll) {

  def get(id: String): Option[LightUser] = try {
    cache get id
  }
  catch {
    case e: java.util.concurrent.ExecutionException =>
      play.api.Logger("light user").warn(s"$id ${e.getMessage}")
      None
  }

  def invalidate = cache invalidate _

  private implicit val lightUserReader = new BSONDocumentReader[LightUser] {

    def read(doc: BSONDocument) = LightUser(
      id = doc.getAs[String](id) err "LightUser id missing",
      name = doc.getAs[String](username) err "LightUser username missing",
      title = doc.getAs[String](title))
  }

  private val cache =
    lila.memo.Builder.cache[String, Option[LightUser]](3 hours, fetch)

  private def fetch(id: String) =
    coll.find(BSONDocument(User.BSONFields.id -> id)).one[LightUser] await 1.second
}
