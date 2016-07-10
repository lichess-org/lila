package lila.history

import org.joda.time.{ DateTime, Days }
import reactivemongo.bson._

import chess.Speed
import lila.db.dsl._
import lila.game.Game
import lila.rating.PerfType
import lila.user.{ User, Perfs }

final class HistoryApi(coll: Coll) {

  import History._

  def add(user: User, game: Game, perfs: Perfs): Funit = {
    val isStd = game.ratingVariant.standard
    val changes = List(
      isStd.option("standard" -> perfs.standard),
      game.ratingVariant.chess960.option("chess960" -> perfs.chess960),
      game.ratingVariant.kingOfTheHill.option("kingOfTheHill" -> perfs.kingOfTheHill),
      game.ratingVariant.threeCheck.option("threeCheck" -> perfs.threeCheck),
      game.ratingVariant.antichess.option("antichess" -> perfs.antichess),
      game.ratingVariant.atomic.option("atomic" -> perfs.atomic),
      game.ratingVariant.horde.option("horde" -> perfs.horde),
      game.ratingVariant.racingKings.option("racingKings" -> perfs.racingKings),
      game.ratingVariant.crazyhouse.option("crazyhouse" -> perfs.crazyhouse),
      (isStd && game.speed == Speed.Bullet).option("bullet" -> perfs.bullet),
      (isStd && game.speed == Speed.Blitz).option("blitz" -> perfs.blitz),
      (isStd && game.speed == Speed.Classical).option("classical" -> perfs.classical),
      (isStd && game.speed == Speed.Correspondence).option("correspondence" -> perfs.correspondence)
    ).flatten.map {
        case (k, p) => k -> p.intRating
      }
    val days = daysBetween(user.createdAt, game.updatedAt | game.createdAt)
    coll.update(
      $id(user.id),
      $doc("$set" -> $doc(changes.map {
        case (perf, rating) => s"$perf.$days" -> $int(rating)
      })),
      upsert = true
    ).void
  }

  def daysBetween(from: DateTime, to: DateTime): Int =
    Days.daysBetween(from.withTimeAtStartOfDay, to.withTimeAtStartOfDay).getDays

  def get(userId: String): Fu[Option[History]] = coll.uno[History]($id(userId))

  def ratingsMap(user: User, perf: PerfType): Fu[RatingsMap] =
    coll.primitiveOne[RatingsMap]($id(user.id), perf.key) map (~_)

  def lastWeekTopRating(user: User, perf: PerfType): Fu[Int] = {
    val days = daysBetween(user.createdAt, DateTime.now minusWeeks 1)
    ratingsMap(user, perf) map { ratings =>
      ratings.foldLeft(user.perfs(perf).intRating) {
        case (rating, (d, r)) if d >= days && r > rating => r
        case (rating, _)                                 => rating
      }
    }
  }
}
