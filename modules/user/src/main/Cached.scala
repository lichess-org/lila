package lila.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject

import lila.db.api.$count
import lila.memo.{ AsyncCache, ExpireSetMemo }
import tube.userTube

final class Cached(
    nbTtl: Duration,
    onlineUserIdMemo: ExpireSetMemo) {

  val count = AsyncCache((o: JsObject) => $count(o), timeToLive = nbTtl)

  def countEnabled: Fu[Int] = count(UserRepo.enabledSelect)

  private def oneDayAgo = DateTime.now minusDays 1
  private def oneWeekAgo = DateTime.now minusWeeks 1
  private def oneMonthAgo = DateTime.now minusMonths 1

  val topRatingDay = AsyncCache(
    (nb: Int) => UserRepo.topRatingSince(oneDayAgo, nb),
    timeToLive = 13 minutes)
  val topRatingWeek = AsyncCache(
    (nb: Int) => UserRepo.topRatingSince(oneWeekAgo, nb),
    timeToLive = 28 minutes)
  // val topRating = AsyncCache(UserRepo.topRating, timeToLive = 30 minutes)
  val topBulletWeek = AsyncCache(UserRepo.topPerfSince("bullet", oneWeekAgo), timeToLive = 31 minutes)
  val topBlitzWeek = AsyncCache(UserRepo.topPerfSince("blitz", oneWeekAgo), timeToLive = 32 minutes)
  val topClassicalWeek = AsyncCache(UserRepo.topPerfSince("classical", oneWeekAgo), timeToLive = 33 minutes)
  val topChess960Week = AsyncCache(UserRepo.topPerfSince("chess960", oneWeekAgo), timeToLive = 36 minutes)
  val topKingOfTheHillWeek = AsyncCache(UserRepo.topPerfSince("kingOfTheHill", oneWeekAgo), timeToLive = 36 minutes)
  val topThreeCheckWeek = AsyncCache(UserRepo.topPerfSince("threeCheck", oneWeekAgo), timeToLive = 36 minutes)
  val topNbGame = AsyncCache(UserRepo.topNbGame, timeToLive = 34 minutes)

  val topPool = AsyncCache(
    (poolIdAndNb: (String, Int)) => UserRepo.topPool(poolIdAndNb._1, poolIdAndNb._2),
    timeToLive = 20 minutes)

  val topOnline = AsyncCache(
    (nb: Int) => UserRepo.byIdsSortRating(onlineUserIdMemo.keys, nb),
    timeToLive = 2 seconds)

  val topToints = AsyncCache(
    (nb: Int) => UserRepo allSortToints nb,
    timeToLive = 10 minutes)
}
