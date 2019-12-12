package lila.history

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import org.joda.time.{ DateTime, Days }
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import chess.Speed
import lila.db.dsl._
import lila.game.Game
import lila.rating.{ Perf, PerfType }
import lila.user.{ User, Perfs }

final class HistoryApi(coll: Coll) {

  import History._

  def addPuzzle(user: User, completedAt: DateTime, perf: Perf): Funit = {
    val days = daysBetween(user.createdAt, completedAt)
    coll.update(
      $id(user.id),
      $set(s"puzzle.$days" -> $int(perf.intRating)),
      upsert = true
    ).void
  }

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
      (isStd && game.speed == Speed.UltraBullet).option("ultraBullet" -> perfs.ultraBullet),
      (isStd && game.speed == Speed.Bullet).option("bullet" -> perfs.bullet),
      (isStd && game.speed == Speed.Blitz).option("blitz" -> perfs.blitz),
      (isStd && game.speed == Speed.Rapid).option("rapid" -> perfs.rapid),
      (isStd && game.speed == Speed.Classical).option("classical" -> perfs.classical),
      (isStd && game.speed == Speed.Correspondence).option("correspondence" -> perfs.correspondence)
    ).flatten.map {
        case (k, p) => k -> p.intRating
      }
    val days = daysBetween(user.createdAt, game.movedAt)
    coll.update(
      $id(user.id),
      $doc("$set" -> $doc(changes.map {
        case (perf, rating) => BSONElement(s"$perf.$days", $int(rating))
      })),
      upsert = true
    ).void
  }

  // used for rating refunds
  def setPerfRating(user: User, perf: PerfType, rating: Int): Funit = {
    val days = daysBetween(user.createdAt, DateTime.now)
    coll.update(
      $id(user.id),
      $set(s"${perf.key}.$days" -> $int(rating))
    ).void
  }

  private def daysBetween(from: DateTime, to: DateTime): Int =
    Days.daysBetween(from.withTimeAtStartOfDay, to.withTimeAtStartOfDay).getDays

  def get(userId: String): Fu[Option[History]] = coll.uno[History]($id(userId))

  def ratingsMap(user: User, perf: PerfType): Fu[RatingsMap] =
    coll.primitiveOne[RatingsMap]($id(user.id), perf.key) map (~_)

  object lastWeekTopRating {

    private case class CacheKey(userId: User.ID, perf: PerfType)

    private val cache: Cache[CacheKey, Int] =
      Scaffeine().expireAfterWrite(10 minutes).build[CacheKey, Int]

    def apply(user: User, perf: PerfType): Fu[Int] = {
      val key = CacheKey(user.id, perf)
      cache.getIfPresent(key) match {
        case Some(rating) => fuccess(rating)
        case None =>
          val currentRating = user.perfs(perf).intRating
          val firstDay = daysBetween(user.createdAt, DateTime.now minusWeeks 1)
          val days = firstDay to (firstDay + 6) toList
          val project = BSONDocument {
            ("_id" -> BSONBoolean(false)) :: days.map { d => s"${perf.key}.$d" -> BSONBoolean(true) }
          }
          coll.find($id(user.id), project).uno[Bdoc](ReadPreference.secondaryPreferred).map {
            _.flatMap {
              _.getAs[Bdoc](perf.key) map {
                _.stream.foldLeft(currentRating) {
                  case (max, scala.util.Success(BSONElement(_, BSONInteger(v)))) if v > max => v
                  case (max, _) => max
                }
              }
            }
          } getOrElse fuccess(currentRating) addEffect { cache.put(key, _) }
      }
    }
  }
}
