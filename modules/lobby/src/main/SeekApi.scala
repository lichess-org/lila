package lila.lobby

import org.joda.time.DateTime
import reactivemongo.core.commands._
import scala.concurrent.duration._

import actorApi.LobbyUser
import lila.db.dsl._
import lila.memo.AsyncCache
import lila.user.{ User, UserRepo }

final class SeekApi(
    coll: Coll,
    archiveColl: Coll,
    blocking: String => Fu[Set[String]],
    maxPerPage: Int,
    maxPerUser: Int) {

  private sealed trait CacheKey
  private object ForAnon extends CacheKey
  private object ForUser extends CacheKey

  private def allCursor =
    coll.find($empty)
      .sort($doc("createdAt" -> -1))
      .cursor[Seek]()

  private val cache = AsyncCache[CacheKey, List[Seek]](
    f = {
      case ForAnon => allCursor.collect[List](maxPerPage)
      case ForUser => allCursor.collect[List]()
    },
    timeToLive = 3.seconds)

  def forAnon = cache(ForAnon)

  def forUser(user: User): Fu[List[Seek]] =
    blocking(user.id) flatMap { blocking =>
      forUser(LobbyUser.make(user, blocking))
    }

  def forUser(user: LobbyUser): Fu[List[Seek]] = cache(ForUser) map { seeks =>
    val filtered = seeks.filter { seek =>
      seek.user.id == user.id || Biter.canJoin(seek, user)
    }
    noDupsFor(user, filtered) take maxPerPage
  }

  private def noDupsFor(user: LobbyUser, seeks: List[Seek]) =
    seeks.foldLeft(List[Seek]() -> Set[String]()) {
      case ((res, h), seek) if seek.user.id == user.id => (seek :: res, h)
      case ((res, h), seek) =>
        val seekH = List(seek.variant, seek.daysPerTurn, seek.mode, seek.color, seek.user.id) mkString ","
        if (h contains seekH) (res, h)
        else (seek :: res, h + seekH)
    }._1.reverse

  def find(id: String): Fu[Option[Seek]] =
    coll.find($doc("_id" -> id)).one[Seek]

  def insert(seek: Seek) = coll.insert(seek) >> findByUser(seek.user.id).flatMap {
    case seeks if seeks.size <= maxPerUser => funit
    case seeks =>
      seeks.drop(maxPerUser).map(remove).sequenceFu
  } >> cache.clear

  def findByUser(userId: String): Fu[List[Seek]] =
    coll.find($doc("user.id" -> userId))
      .sort($doc("createdAt" -> -1))
      .cursor[Seek]().collect[List]()

  def remove(seek: Seek) =
    coll.remove($doc("_id" -> seek.id)).void >> cache.clear

  def archive(seek: Seek, gameId: String) = {
    val archiveDoc = Seek.seekBSONHandler.write(seek) ++ $doc(
      "gameId" -> gameId,
      "archivedAt" -> DateTime.now)
    coll.remove($doc("_id" -> seek.id)).void >>
      cache.clear >>
      archiveColl.insert(archiveDoc)
  }

  def findArchived(gameId: String): Fu[Option[Seek]] =
    archiveColl.find($doc("gameId" -> gameId)).one[Seek]

  def removeBy(seekId: String, userId: String) =
    coll.remove($doc(
      "_id" -> seekId,
      "user.id" -> userId
    )).void >> cache.clear
}
