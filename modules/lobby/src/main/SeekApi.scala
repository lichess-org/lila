package lila.lobby
import lila.core.perf.UserWithPerfs
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

final class SeekApi(
    userApi: lila.core.user.UserApi,
    config: SeekApi.Config,
    biter: Biter,
    relationApi: lila.core.relation.RelationApi,
    cacheApi: lila.memo.CacheApi
)(using Executor):
  import config.*

  private type CacheKey = Boolean
  private val ForAnon = false
  private val ForUser = true

  private def allCursor =
    coll
      .find($empty)
      .sort($sort.desc("createdAt"))
      .cursor[Seek]()

  private val cache = cacheApi[CacheKey, List[Seek]](2, "lobby.seek.list"):
    _.refreshAfterWrite(3.seconds)
      .buildAsyncFuture:
        if _ then allCursor.list(500)
        else allCursor.list(maxPerPage.value)

  private def cacheClear() =
    cache.invalidate(ForAnon)
    cache.invalidate(ForUser)

  def forAnon = cache.get(ForAnon)

  def forMe(using me: User | UserWithPerfs): Fu[List[Seek]] = for
    user <- me match
      case u: UserWithPerfs => fuccess(u)
      case u: User => userApi.withPerfs(u)
    blocking <- relationApi.fetchBlocking(user.id)
    seeks <- forUser(LobbyUser.make(user, lila.core.pool.Blocking(blocking)))
  yield seeks

  def forUser(user: LobbyUser): Fu[List[Seek]] =
    cache.get(ForUser).map { seeks =>
      val filtered = seeks.filter: seek =>
        seek.user.is(user) || biter.canJoin(seek, user)
      noDupsFor(user, filtered).take(maxPerPage.value)
    }

  private def noDupsFor(user: LobbyUser, seeks: List[Seek]) =
    seeks
      .foldLeft(List.empty[Seek] -> Set.empty[String]):
        case ((res, h), seek) if seek.user.id == user.id => (seek :: res, h)
        case ((res, h), seek) =>
          val seekH = List(seek.variant, seek.daysPerTurn, seek.rated.name, seek.user.id).mkString(",")
          if h contains seekH then (res, h)
          else (seek :: res, h + seekH)
      ._1
      .reverse

  def find(id: String): Fu[Option[Seek]] =
    coll.find($id(id)).one[Seek]

  def insert(seek: Seek) = for
    _ <- coll.insert.one(seek)
    seeks <- findByUser(seek.user.id)
    _ <-
      if seeks.sizeIs <= maxPerUser.value then funit
      else seeks.drop(maxPerUser.value).sequentiallyVoid(remove)
  yield cacheClear()

  def findByUser(userId: UserId): Fu[List[Seek]] =
    coll
      .find($doc("user.id" -> userId))
      .sort($sort.desc("createdAt"))
      .cursor[Seek]()
      .list(100)

  def remove(seek: Seek) =
    for _ <- coll.delete.one($doc("_id" -> seek.id))
    yield cacheClear()

  def archive(seek: Seek, gameId: GameId) = for
    _ <- coll.delete.one($id(seek.id))
    archiveDoc = bsonWriteObjTry[Seek](seek).get ++ $doc(
      "gameId" -> gameId,
      "archivedAt" -> nowInstant
    )
    _ <- archiveColl.insert.one(archiveDoc)
  yield cacheClear()

  def findArchived(gameId: GameId): Fu[Option[Seek]] =
    archiveColl.find($doc("gameId" -> gameId)).one[Seek]

  def removeBy(seekId: String, userId: UserId) =
    for _ <- coll.delete.one($doc("_id" -> seekId, "user.id" -> userId))
    yield cacheClear()

  def removeByUser(user: User) =
    for _ <- coll.delete.one($doc("user.id" -> user.id)) yield cacheClear()

private object SeekApi:

  final class Config(
      val coll: Coll,
      val archiveColl: Coll,
      val maxPerPage: MaxPerPage,
      val maxPerUser: Max
  )
