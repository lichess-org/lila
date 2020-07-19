package lila.report

import lila.user.User
import org.joda.time.DateTime

case class TempThreshold(score: Int, lastUsed: DateTime)

final class ModReportFilter {

  // mutable storage, because I cba to put it in DB
  private var modIdFilter = Map.empty[User.ID, Option[Room]]
  // mutable storage, because I also cba to put it in DB
  // Also, this value shouldn't be in the DB. If we ever move the other value to the
  // db we should move this to a cookie or something. I intend
  // for this to be a temporary cookie of some sort.
  private var modTempThreshold = Map.empty[User.ID, Option[TempThreshold]]

  def get(mod: User): Option[Room] = modIdFilter.get(mod.id).flatten

  def set(mod: User, filter: Option[Room]) =
    modIdFilter = modIdFilter + (mod.id -> filter)

  def getThreshold(mod: User): Option[Int] =
    modTempThreshold
      .get(mod.id)
      .flatten
      .filter(_.lastUsed isAfter DateTime.now.minusHours(1))
      .map(t => {
        modTempThreshold = modTempThreshold + (mod.id -> some(TempThreshold(t.score, DateTime.now)))
        t.score
      })

  def updateThreshold(mod: User, score: Option[Int]): Option[Int] =
    score.fold(getThreshold(mod))(score => {
      modTempThreshold = modTempThreshold + (mod.id -> some(TempThreshold(score, DateTime.now)))
      some(score)
    })
}
