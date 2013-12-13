package lila.user

import scala.concurrent.duration._

import play.api.libs.json.JsObject

import lila.db.api.$count
import lila.memo.{ AsyncCache, ExpireSetMemo }
import tube.userTube

final class Cached(
    nbTtl: Duration,
    eloChartTtl: Duration,
    onlineUserIdMemo: ExpireSetMemo) {

  val username = AsyncCache(UserRepo.usernameById, maxCapacity = 50000)

  def usernameOrAnonymous(id: String): Fu[String] =
    username(id) map (_ | User.anonymous)

  def usernameOrAnonymous(id: Option[String]): Fu[String] =
    id.fold(fuccess(User.anonymous))(usernameOrAnonymous)

  val count = AsyncCache((o: JsObject) ⇒ $count(o), timeToLive = nbTtl)

  def countEnabled: Fu[Int] = count(UserRepo.enabledSelect)

  val eloChart = AsyncCache(EloChart.apply,
    maxCapacity = 5000,
    timeToLive = eloChartTtl)

  private val topListTtl = 1 minute
  val topElo = AsyncCache(UserRepo.topElo, timeToLive = topListTtl)
  val topBullet = AsyncCache(UserRepo.topBullet, timeToLive = topListTtl)
  val topBlitz = AsyncCache(UserRepo.topBlitz, timeToLive = topListTtl)
  val topSlow = AsyncCache(UserRepo.topSlow, timeToLive = topListTtl)
  val topNbGame = AsyncCache(UserRepo.topNbGame, timeToLive = topListTtl)

  val topOnline = AsyncCache(
    (nb: Int) ⇒ UserRepo.byIdsSortElo(onlineUserIdMemo.keys, nb),
    timeToLive = 2 seconds)
}
