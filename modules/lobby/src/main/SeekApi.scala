package lila.lobby

import lila.common.config.*
import lila.db.dsl.{ *, given }
import lila.user.User
import lila.memo.CacheApi.*

final class SeekApi(
    config: SeekApi.Config,
    biter: Biter,
    relationApi: lila.relation.RelationApi,
    perfsRepo: lila.user.UserPerfsRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):
  import config.*

  private type CacheKey = Boolean
  private val ForAnon = false
  private val ForUser = true

  private def allCursor =
    coll
      .find($empty)
      .sort($sort desc "createdAt")
      .cursor[Seek]()

  private val cache = cacheApi[CacheKey, List[Seek]](2, "lobby.seek.list") {
    _.refreshAfterWrite(3 seconds)
      .buildAsyncFuture {
        if _ then allCursor.list(500)
        else allCursor.list(maxPerPage.value)
      }
  }

  private def cacheClear() =
    cache invalidate ForAnon
    cache invalidate ForUser

  def forAnon = cache get ForAnon

  def forMe(using me: User | User.WithPerfs): Fu[List[Seek]] = for
    user <- me match
      case u: User.WithPerfs => fuccess(u)
      case u: User           => perfsRepo.withPerfs(u)
    blocking <- relationApi.fetchBlocking(user.id)
    seeks    <- forUser(LobbyUser.make(user, lila.pool.Blocking(blocking)))
  yield seeks

  def forUser(user: LobbyUser): Fu[List[Seek]] =
    cache get ForUser map { seeks =>
      val filtered = seeks.filter: seek =>
        seek.user.is(user) || biter.canJoin(seek, user)
      noDupsFor(user, filtered) take maxPerPage.value
    }

  private def noDupsFor(user: LobbyUser, seeks: List[Seek]) =
    seeks
      .foldLeft(List.empty[Seek] -> Set.empty[String]) {
        case ((res, h), seek) if seek.user.id == user.id => (seek :: res, h)
        case ((res, h), seek) =>
          val seekH = List(seek.variant, seek.daysPerTurn, seek.mode, seek.color, seek.user.id) mkString ","
          if h contains seekH then (res, h)
          else (seek :: res, h + seekH)
      }
      ._1
      .reverse

  def find(id: String): Fu[Option[Seek]] =
    coll.find($id(id)).one[Seek]

  def insert(seek: Seek) =
    coll.insert.one(seek) >> findByUser(seek.user.id).flatMap {
      case seeks if seeks.sizeIs <= maxPerUser.value => funit
      case seeks                                     => seeks.drop(maxPerUser.value).map(remove).parallel
    }.void andDo cacheClear()

  def findByUser(userId: UserId): Fu[List[Seek]] =
    coll
      .find($doc("user.id" -> userId))
      .sort($sort desc "createdAt")
      .cursor[Seek]()
      .list(100)

  def remove(seek: Seek) =
    coll.delete.one($doc("_id" -> seek.id)).void andDo cacheClear()

  def archive(seek: Seek, gameId: GameId) =
    val archiveDoc = bsonWriteObjTry[Seek](seek).get ++ $doc(
      "gameId"     -> gameId,
      "archivedAt" -> nowInstant
    )
    coll.delete.one($doc("_id" -> seek.id)).void >>
      archiveColl.insert.one(archiveDoc) andDo
      cacheClear()

  def findArchived(gameId: GameId): Fu[Option[Seek]] =
    archiveColl.find($doc("gameId" -> gameId)).one[Seek]

  def removeBy(seekId: String, userId: UserId) =
    coll.delete
      .one(
        $doc(
          "_id"     -> seekId,
          "user.id" -> userId
        )
      )
      .void andDo cacheClear()

  def removeByUser(user: lila.user.User) =
    coll.delete.one($doc("user.id" -> user.id)).void andDo cacheClear()

private object SeekApi:

  final class Config(
      val coll: Coll,
      val archiveColl: Coll,
      val maxPerPage: MaxPerPage,
      val maxPerUser: Max
  )
