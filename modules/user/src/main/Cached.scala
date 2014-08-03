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

  private def oneDayAgo = DateTime.now minusDays 1
  // private def oneDayAgo = DateTime.now minusMonths 6
  private def twoWeeksAgo = DateTime.now minusWeeks 2
  // private def twoWeeksAgo = DateTime.now minusMonths 6

  private val perfs = PerfType.nonPoolPuzzle
  private val perfKeys = perfs.map(_.key)

  val count = AsyncCache((o: JsObject) => $count(o), timeToLive = nbTtl)

  def countEnabled: Fu[Int] = count(UserRepo.enabledSelect)

  val leaderboardSize = 10
  def activeSince = DateTime.now minusWeeks 2

  val topPerf = AsyncCache[Perf.Key, List[User]](
    f = (perf: Perf.Key) => UserRepo.topPerfSince(perf, twoWeeksAgo, leaderboardSize),
    timeToLive = 30 minutes)

  val topToday = AsyncCache.single[List[(User, PerfType)]](
    f = perfs.map { perf =>
      UserRepo.topPerfSince(perf.key, oneDayAgo, 1) map2 { (u: User) => u -> perf }
    }.sequenceFu map (_.flatten),
    timeToLive = 29 minutes)

  val topNbGame = AsyncCache(UserRepo.topNbGame, timeToLive = 34 minutes)

  val topPool = AsyncCache(
    (poolIdAndNb: (User.ID, Int)) => UserRepo.topPool(poolIdAndNb._1, poolIdAndNb._2),
    timeToLive = 20 minutes)

  val topOnline = AsyncCache(
    (nb: Int) => UserRepo.byIdsSortRating(onlineUserIdMemo.keys, nb),
    timeToLive = 2 seconds)

  val topToints = AsyncCache(
    (nb: Int) => UserRepo allSortToints nb,
    timeToLive = 10 minutes)

  object ranking {

    def getAll(id: User.ID): Fu[Map[Perf.Key, Int]] = perfKeys.map { perf =>
      cache(perf) map { _ get id map (perf -> _) }
    }.sequenceFu map (_.flatten.toMap)

    private val cache = AsyncCache(compute, timeToLive = 31 minutes)

    private def compute(perf: Perf.Key): Fu[Map[User.ID, Int]] =
      $primitive(
        UserRepo.topPerfSinceSelect(perf, twoWeeksAgo),
        "_id",
        _ sort UserRepo.sortPerfDesc(perf)
      )(_.asOpt[User.ID]) map { _.zipWithIndex.map(x => x._1 -> (x._2 + 1)).toMap }
  }
}
