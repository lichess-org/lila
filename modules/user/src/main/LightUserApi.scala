package lila.user

import reactivemongo.api.bson._
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }

import lila.common.LightUser
import lila.db.dsl._
import lila.memo.Syncache
import User.{ BSONFields => F }

final class LightUserApi(repo: UserRepo)(implicit system: akka.actor.ActorSystem) {

  import LightUserApi._

  def sync(id: User.ID): Option[LightUser] = cache sync id
  def async(id: User.ID): Fu[Option[LightUser]] = cache async id

  def asyncMany = cache.asyncMany _

  def invalidate = cache invalidate _

  def preloadOne = cache preloadOne _
  def preloadMany = cache preloadMany _
  def preloadUser(user: User) = cache.setOneIfAbsent(user.id, user.light.some)

  def isBotSync(id: User.ID) = sync(id).exists(_.isBot)

  private val cacheName = "user.light"

  private val cache = new Syncache[User.ID, Option[LightUser]](
    name = cacheName,
    compute = id => repo.coll.find($id(id), projection.some).uno[LightUser],
    default = id => LightUser(id, id, None, false).some,
    strategy = Syncache.WaitAfterUptime(10 millis),
    expireAfter = Syncache.ExpireAfterAccess(15 minutes),
    logger = logger branch "LightUserApi"
  )

  def monitorCache = lila.mon.syncache.chmSize(cacheName)(cache.chmSize)
}

private object LightUserApi {

  implicit val lightUserBSONReader = new BSONDocumentReader[LightUser] {

    def readDocument(doc: BSONDocument) = Success(LightUser(
      id = doc.string(F.id) err "LightUser id missing",
      name = doc.string(F.username) err "LightUser username missing",
      title = doc.string(F.title),
      isPatron = ~doc.child(F.plan).flatMap(_.getAsOpt[Boolean]("active"))
    ))
  }

  val projection = $doc(F.username -> true, F.title -> true, s"${F.plan}.active" -> true)
}
