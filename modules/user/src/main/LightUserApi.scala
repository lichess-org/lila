package lila.user

import reactivemongo.api.bson.*
import scala.concurrent.duration.*
import scala.util.Success

import lila.common.LightUser
import lila.db.dsl.{ given, * }
import lila.memo.{ CacheApi, Syncache }
import User.BSONFields as F

final class LightUserApi(
    repo: UserRepo,
    cacheApi: CacheApi
)(using ec: scala.concurrent.ExecutionContext):

  import LightUserApi.{ *, given }

  val async = LightUser.Getter(id => if (User isGhost id) fuccess(LightUser.ghost.some) else cache.async(id))
  val sync  = LightUser.GetterSync(id => if (User isGhost id) LightUser.ghost.some else cache.sync(id))

  def async(id: UserId) = if (User isGhost id.value) fuccess(LightUser.ghost.some) else cache.async(id.value)

  def syncFallback(id: User.ID)       = sync(id) | LightUser.fallback(id)
  def asyncFallback(id: User.ID)      = async(id) dmap (_ | LightUser.fallback(id))
  def asyncFallbackName(name: String) = async(User normalize name) dmap (_ | LightUser.fallback(name))

  def asyncMany = cache.asyncMany

  def asyncManyFallback(ids: Seq[User.ID]): Fu[Seq[LightUser]] =
    ids.map(asyncFallback).sequenceFu

  val isBotSync: LightUser.IsBotSync = LightUser.IsBotSync(id => sync(id).exists(_.isBot))

  def invalidate = cache.invalidate

  def preloadOne                     = cache.preloadOne
  def preloadMany                    = cache.preloadMany
  def preloadUser(user: User)        = cache.set(user.id, user.light.some)
  def preloadUsers(users: Seq[User]) = users.foreach(preloadUser)

  private val cache = cacheApi.sync[User.ID, Option[LightUser]](
    name = "user.light",
    initialCapacity = 1024 * 1024,
    compute = id =>
      if (User isGhost id) fuccess(LightUser.ghost.some)
      else
        repo.coll.find($id(id), projection).one[LightUser] recover {
          case _: reactivemongo.api.bson.exceptions.BSONValueNotFoundException => LightUser.ghost.some
        },
    default = id => LightUser(id, id, None, isPatron = false).some,
    strategy = Syncache.WaitAfterUptime(10 millis),
    expireAfter = Syncache.ExpireAfterWrite(20 minutes)
  )

private object LightUserApi:

  given BSONDocumentReader[LightUser] with
    def readDocument(doc: BSONDocument) = for {
      id   <- doc.getAsTry[String](F.id)
      name <- doc.getAsTry[String](F.username)
    } yield LightUser(
      id = id,
      name = name,
      title = doc.getAsOpt[UserTitle](F.title),
      isPatron = ~doc.child(F.plan).flatMap(_.getAsOpt[Boolean]("active"))
    )

  val projection = $doc(F.username -> true, F.title -> true, s"${F.plan}.active" -> true).some
