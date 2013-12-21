package lila.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject

import lila.db.api.$count
import lila.memo.{ AsyncCache, ExpireSetMemo }
import tube.userTube

final class Cached(
    nbTtl: Duration,
    ratingChartTtl: Duration,
    onlineUserIdMemo: ExpireSetMemo) {

  val username = AsyncCache(UserRepo.usernameById, maxCapacity = 50000)

  def usernameOrAnonymous(id: String): Fu[String] =
    username(id) map (_ | User.anonymous)

  def usernameOrAnonymous(id: Option[String]): Fu[String] =
    id.fold(fuccess(User.anonymous))(usernameOrAnonymous)

  val count = AsyncCache((o: JsObject) ⇒ $count(o), timeToLive = nbTtl)

  def countEnabled: Fu[Int] = count(UserRepo.enabledSelect)

  val ratingChart = AsyncCache(RatingChart.apply,
    maxCapacity = 5000,
    timeToLive = ratingChartTtl)

  private val topListTtl = 5 minutes
  // private val topListTtl = 1 second
  // private val topListSinceTtl = 1 second
  private def oneDayAgo = DateTime.now minusDays 1
  private def oneWeekAgo = DateTime.now minusWeeks 1
  private def oneMonthAgo = DateTime.now minusMonths 1
  val topProgressDay = AsyncCache(
    UserRepo.topProgressSince(oneDayAgo),
    timeToLive = 14 minutes)
  val topProgressWeek = AsyncCache(
    UserRepo.topProgressSince(oneWeekAgo),
    timeToLive = 29 minutes)
  val topProgressMonth = AsyncCache(
    UserRepo.topProgressSince(oneMonthAgo),
    timeToLive = 27 minutes)
  val topRating = AsyncCache(UserRepo.topRating, timeToLive = topListTtl)
  val topRatingDay = AsyncCache(
    UserRepo.topRatingSince(oneDayAgo),
    timeToLive = 13 minutes)
  val topRatingWeek = AsyncCache(
    UserRepo.topRatingSince(oneWeekAgo),
    timeToLive = 28 minutes)
  val topBullet = AsyncCache(UserRepo.topBullet, timeToLive = topListTtl)
  val topBlitz = AsyncCache(UserRepo.topBlitz, timeToLive = topListTtl)
  val topSlow = AsyncCache(UserRepo.topSlow, timeToLive = topListTtl)
  val topNbGame = AsyncCache(UserRepo.topNbGame, timeToLive = topListTtl)

  val topOnline = AsyncCache(
    (nb: Int) ⇒ UserRepo.byIdsSortRating(onlineUserIdMemo.keys, nb),
    timeToLive = 2 seconds)
}
