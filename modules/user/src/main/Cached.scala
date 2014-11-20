package lila.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import reactivemongo.bson._

import lila.db.api.{ $count, $primitive }
import lila.db.Implicits._
import lila.memo.{ AsyncCache, ExpireSetMemo }
import lila.rating.{ Perf, PerfType }
import tube.userTube

final class Cached(
    nbTtl: Duration,
    onlineUserIdMemo: ExpireSetMemo) {

  private def twoWeeksAgo = DateTime.now minusWeeks 2

  private val perfs = PerfType.nonPuzzle
  private val perfKeys = perfs.map(_.key)

  private val countCache = AsyncCache.single($count(UserRepo.enabledSelect), timeToLive = nbTtl)

  def countEnabled: Fu[Int] = countCache(true)

  val leaderboardSize = 10
  def activeSince = DateTime.now minusWeeks 2

  val topPerf = AsyncCache[Perf.Key, List[User]](
    f = (perf: Perf.Key) => UserRepo.topPerfSince(perf, twoWeeksAgo, leaderboardSize),
    timeToLive = 10 minutes)

  val topToday = AsyncCache.single[List[(User, PerfType)]](
    f = perfs.map { perf =>
      UserRepo.topPerfSince(perf.key, DateTime.now minusHours 12, 1) map2 { (u: User) => u -> perf }
    }.sequenceFu map (_.flatten),
    timeToLive = 10 minutes)

  val topNbGame = AsyncCache[Int, List[User]](
    UserRepo.topNbGame,
    timeToLive = 34 minutes)

  val topOnline = AsyncCache[Int, List[User]](
    UserRepo.byIdsSortRating(onlineUserIdMemo.keys, _),
    timeToLive = 3 seconds)

  val topToints = AsyncCache[Int, List[User]](
    UserRepo.allSortToints,
    timeToLive = 10 minutes)

  object ranking {

    def getAll(id: User.ID): Fu[Map[Perf.Key, Int]] = perfKeys.map { perf =>
      cache(perf) map { _ get id map (perf -> _) }
    }.sequenceFu map (_.flatten.toMap)

    private val cache = AsyncCache[Perf.Key, Map[User.ID, Int]](compute, timeToLive = 31 minutes)

    private def compute(perf: Perf.Key): Fu[Map[User.ID, Int]] =
      $primitive(
        UserRepo.topPerfSinceSelect(perf, twoWeeksAgo),
        "_id",
        _ sort UserRepo.sortPerfDesc(perf)
      )(_.asOpt[User.ID]) map { _.zipWithIndex.map(x => x._1 -> (x._2 + 1)).toMap }
  }
}
