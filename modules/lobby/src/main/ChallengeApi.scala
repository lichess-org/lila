package lila.lobby

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import reactivemongo.core.commands._
import scala.concurrent.duration._

import actorApi.LobbyUser
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import lila.memo.AsyncCache
import lila.user.{ User, UserRepo }

final class ChallengeApi(coll: Coll) {

  private def allCursor =
    coll.find(BSONDocument())
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]()

  def forUser(user: User): Fu[List[Challenge]] =
    blocking(user.id) flatMap { blocking =>
      forUser(LobbyUser.make(user, blocking))
    }

  def forUser(user: LobbyUser): Fu[List[Challenge]] = cache(ForUser) map { challenges =>
    val filtered = challenges.filter { challenge =>
      challenge.user.id == user.id || Biter.canJoin(challenge, user)
    }
    noDupsFor(user, filtered) take maxPerPage
  }

  private def noDupsFor(user: LobbyUser, challenges: List[Challenge]) =
    challenges.foldLeft(List[Challenge]() -> Set[String]()) {
      case ((res, h), challenge) if challenge.user.id == user.id => (challenge :: res, h)
      case ((res, h), challenge) =>
        val challengeH = List(challenge.variant, challenge.daysPerTurn, challenge.mode, challenge.color, challenge.user.id) mkString ","
        if (h contains challengeH) (res, h)
        else (challenge :: res, h + challengeH)
    }._1.reverse

  def find(id: String): Fu[Option[Challenge]] =
    coll.find(BSONDocument("_id" -> id)).one[Challenge]

  def insert(challenge: Challenge) = coll.insert(challenge) >> findByUser(challenge.user.id).flatMap {
    case challenges if challenges.size <= maxPerUser => funit
    case challenges =>
      challenges.drop(maxPerUser).map(remove).sequenceFu
  } >> cache.clear

  def findByUser(userId: String): Fu[List[Challenge]] =
    coll.find(BSONDocument("user.id" -> userId))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[List]()

  def remove(challenge: Challenge) =
    coll.remove(BSONDocument("_id" -> challenge.id)).void >> cache.clear

  def archive(challenge: Challenge, gameId: String) = {
    val archiveDoc = Challenge.challengeBSONHandler.write(challenge) ++ BSONDocument(
      "gameId" -> gameId,
      "archivedAt" -> DateTime.now)
    coll.remove(BSONDocument("_id" -> challenge.id)).void >>
      cache.clear >>
      archiveColl.insert(archiveDoc)
  }

  def findArchived(gameId: String): Fu[Option[Challenge]] =
    archiveColl.find(BSONDocument("gameId" -> gameId)).one[Challenge]

  def removeBy(challengeId: String, userId: String) =
    coll.remove(BSONDocument(
      "_id" -> challengeId,
      "user.id" -> userId
    )).void >> cache.clear
}
