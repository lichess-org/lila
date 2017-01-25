package lila.user

import reactivemongo.bson._
import scala.concurrent.duration._

import lila.common.LightUser
import lila.db.dsl._
import lila.memo.Syncache
import User.{ BSONFields => F }

final class LightUserApi(coll: Coll)(implicit system: akka.actor.ActorSystem) {

  def sync(id: String): Option[LightUser] = cache sync id
  def async(id: String): Fu[Option[LightUser]] = cache async id

  def invalidate = cache invalidate _

  def preloadOne = cache preloadOne _
  def preloadMany = cache preloadMany _

  def getList(ids: List[String]): List[LightUser] = ids flatMap sync

  def usernameList(ids: List[String]): List[String] = getList(ids).map(_.name)

  private implicit val lightUserReader = new BSONDocumentReader[LightUser] {

    def read(doc: BSONDocument) = LightUser(
      id = doc.getAs[String](F.id) err "LightUser id missing",
      name = doc.getAs[String](F.username) err "LightUser username missing",
      title = doc.getAs[String](F.title),
      isPatron = ~doc.getAs[Bdoc](F.plan).flatMap(_.getAs[Boolean]("active")))
  }

  private val projection = $doc(F.username -> true, F.title -> true, s"${F.plan}.active" -> true)

  private val cache = new Syncache[String, Option[LightUser]](
    name = "user.light",
    compute = id => coll.find($id(id), projection).uno[LightUser],
    default = id => LightUser(id, id, None, false).some,
    strategy = Syncache.WaitAfterUptime(10 millis),
    timeToLive = 20 minutes,
    logger = logger branch "LightUserApi")
}
