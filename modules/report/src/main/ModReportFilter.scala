package lila.report

import lila.user.User
import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration.FiniteDuration

case class TempThreshold(score: Int)

final class ModReportFilter {

  // mutable storage, because I cba to put it in DB
  private var modIdFilter = Map.empty[User.ID, Option[Room]]
  // mutable storage, because I also cba to put it in DB
  // Also, this value shouldn't be in the DB. If we ever move the other value to the
  // db we should move this to a cookie or something. I intend
  // for this to be a temporary cookie of some sort.
  private val modTempThreshold: Cache[User.ID, TempThreshold] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(FiniteDuration(1, "hour"))
    .build[User.ID, TempThreshold]()

  def get(mod: User): Option[Room] = modIdFilter.get(mod.id).flatten

  def set(mod: User, filter: Option[Room]) =
    modIdFilter = modIdFilter + (mod.id -> filter)

  def getThreshold(mod: User): Option[Int] =
    modTempThreshold
      .getIfPresent(mod.id)
      .map(_.score)

  def updateThreshold(mod: User, score: Option[Int]): Option[Int] =
    score.fold(getThreshold(mod))(score => {
      modTempThreshold.put(mod.id, TempThreshold(score))
      some(score)
    })
}
