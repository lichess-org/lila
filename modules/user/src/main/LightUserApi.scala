package lila.user

import reactivemongo.api.bson.*

import lila.core.LightUser
import lila.db.dsl.{ *, given }
import lila.memo.{ CacheApi, Syncache }

import BSONFields as F

final class LightUserApi(repo: UserRepo, cacheApi: CacheApi)(using Executor)
    extends lila.core.user.LightUserApi:

  val async = LightUser.Getter: id =>
    if id.isGhost then fuccess(LightUser.ghost.some) else cache.async(id)
  val asyncFallback = LightUser.GetterFallback: id =>
    if id.isGhost then fuccess(LightUser.ghost)
    else cache.async(id).dmap(_ | LightUser.fallback(id.into(UserName)))
  val sync = LightUser.GetterSync: id =>
    if id.isGhost then LightUser.ghost.some else cache.sync(id)
  val syncFallback = LightUser.GetterSyncFallback: id =>
    if id.isGhost then LightUser.ghost else cache.sync(id) | LightUser.fallback(id.into(UserName))

  export cache.{ asyncMany, invalidate, preloadOne, preloadMany }

  def asyncFallbackName(name: UserName) = async(name.id).dmap(_ | LightUser.fallback(name))

  def asyncManyFallback(ids: Seq[UserId]): Fu[Seq[LightUser]] = ids.parallel(asyncFallback)

  def asyncManyOptions(ids: Seq[Option[UserId]]): Fu[Seq[Option[LightUser]]] = ids.parallel(_.so(async))

  val isBotSync: LightUser.IsBotSync = LightUser.IsBotSync(id => sync(id).exists(_.isBot))

  def preloadUser(user: User): Unit = cache.set(user.id, user.light.some)
  def preloadUsers(users: Seq[User]): Unit = users.foreach(preloadUser)

  private val cache: Syncache[UserId, Option[LightUser]] = cacheApi.sync[UserId, Option[LightUser]](
    name = "user.light",
    initialCapacity = 1024 * 1024,
    compute = id =>
      if id.isGhost then fuccess(LightUser.ghost.some)
      else
        repo.coll
          .find($id(id), projection)
          .one[LightUser]
          .recover:
            case _: reactivemongo.api.bson.exceptions.BSONValueNotFoundException => LightUser.ghost.some
    ,
    default = id => LightUser(id, id.into(UserName), None, None, isPatron = false).some,
    strategy = Syncache.Strategy.WaitAfterUptime(10.millis),
    expireAfter = Syncache.ExpireAfter.Write(20.minutes)
  )

  private given BSONDocumentReader[LightUser] with
    def readDocument(doc: BSONDocument) =
      doc
        .getAsTry[UserName](F.username)
        .map: name =>
          LightUser(
            id = name.id,
            name = name,
            title = doc.getAsOpt[chess.PlayerTitle](F.title),
            flair = doc.getAsOpt[Flair](F.flair).filter(FlairApi.exists),
            isPatron = ~doc.child(F.plan).flatMap(_.getAsOpt[Boolean]("active"))
          )

  private val projection =
    $doc(
      F.id -> false,
      F.username -> true,
      F.title -> true,
      s"${F.plan}.active" -> true,
      F.flair -> true
    ).some
