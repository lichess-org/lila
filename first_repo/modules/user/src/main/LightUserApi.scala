package lila.user

import reactivemongo.api.bson._
import scala.concurrent.duration._
import scala.util.Success

import lila.common.LightUser
import lila.db.dsl._
import lila.memo.{ CacheApi, Syncache }
import User.{ BSONFields => F }

final class LightUserApi(
    repo: UserRepo,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import LightUserApi._

  val async = new LightUser.Getter(cache.async)
  val sync  = new LightUser.GetterSync(cache.sync)

  def syncFallback(id: User.ID)  = sync(id) | LightUser.fallback(id)
  def asyncFallback(id: User.ID) = async(id) dmap (_ | LightUser.fallback(id))

  def asyncMany = cache.asyncMany _

  def invalidate = cache invalidate _

  def preloadOne                     = cache preloadOne _
  def preloadMany                    = cache preloadMany _
  def preloadUser(user: User)        = cache.set(user.id, user.light.some)
  def preloadUsers(users: Seq[User]) = users.foreach(preloadUser)

  private val cache = cacheApi.sync[User.ID, Option[LightUser]](
    name = "user.light",
    initialCapacity = 131072,
    compute = id => repo.coll.find($id(id), projection).one[LightUser],
    default = id => LightUser(id, id, None, isPatron = false).some,
    strategy = Syncache.WaitAfterUptime(8 millis),
    expireAfter = Syncache.ExpireAfterWrite(20 minutes)
  )
}

private object LightUserApi {

  implicit val lightUserBSONReader = new BSONDocumentReader[LightUser] {

    def readDocument(doc: BSONDocument) =
      Success(
        LightUser(
          id = doc.string(F.id) err "LightUser id missing",
          name = doc.string(F.username) err "LightUser username missing",
          title = doc.string(F.title),
          isPatron = ~doc.child(F.plan).flatMap(_.getAsOpt[Boolean]("active"))
        )
      )
  }

  val projection = $doc(F.username -> true, F.title -> true, s"${F.plan}.active" -> true).some
}
