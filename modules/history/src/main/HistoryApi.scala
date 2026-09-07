package lila.history

import chess.Speed
import chess.IntRating
import reactivemongo.api.bson.*
import scalalib.model.Days

import lila.core.perf.UserPerfs
import lila.db.AsyncCollFailingSilently
import lila.db.dsl.{ *, given }

final class HistoryApi(
    withColl: AsyncCollFailingSilently,
    userApi: lila.core.user.UserApi,
    cacheApi: lila.memo.CacheApi
)(using Executor)
    extends lila.core.history.HistoryApi:

  import History.given

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    withColl(_.delete.one($id(del.id)).void)

  def addPuzzle(user: User, completedAt: Instant, perf: lila.core.perf.Perf): Funit =
    withColl: coll =>
      val days = daysBetween(user.createdAt, completedAt)
      coll.update
        .one(
          $id(user.id),
          $set(s"puzzle.$days" -> perf.intRating),
          upsert = true
        )
        .void

  def add(user: User, game: Game, perfs: UserPerfs): Funit = withColl: coll =>
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
    ).flatten.map: (k, p) =>
      k -> p.intRating
    val days = daysBetween(user.createdAt, game.movedAt)
    coll.update
      .one(
        $id(user.id),
        $doc("$set" -> $doc(changes.map: (perf, rating) =>
          (s"$perf.$days", $int(rating)))),
        upsert = true
      )
      .void

  // used for rating refunds
  def setPerfRating(user: User, perf: PerfKey, rating: IntRating): Funit = withColl: coll =>
    val days = daysBetween(user.createdAt, nowInstant)
    coll.update
      .one(
        $id(user.id),
        $set(s"$perf.$days" -> $int(rating))
      )
      .void

  private def daysBetween(from: Instant, to: Instant): Int =
    scalalib.time.daysBetween(from.withTimeAtStartOfDay, to.withTimeAtStartOfDay)

  def get(userId: UserId): Fu[Option[History]] = withColl(_.one[History]($id(userId)))

  def ratingsMap[U: UserIdOf](user: U, perf: PerfKey): Fu[RatingsMap] =
    withColl(_.primitiveOne[RatingsMap]($id(user.id), perf.value).dmap(~_))

  def progresses(
      users: List[lila.core.user.WithPerf],
      perfKey: PerfKey,
      days: Days
  ): Fu[List[PairOf[IntRating]]] =
    withColl:
      _.optionsByOrderedIds[Bdoc, UserId](
        users.map(_.id),
        $doc(perfKey.value -> true).some
      )(_.getAsTry[UserId]("_id").get).map { hists =>
        import History.ratingsReader
        users.zip(hists).map { (user, doc) =>
          val current = user.perf.intRating
          val previousDate = daysBetween(user.createdAt, nowInstant.minusDays(days.value))
          val previous =
            doc
              .flatMap(_.child(perfKey.value))
              .flatMap(ratingsReader.readOpt)
              .fold(current): hist =>
                hist.foldLeft(hist.headOption.fold(current)(_._2)):
                  case (_, (d, r)) if d < previousDate => r
                  case (acc, _) => acc
          previous -> current
        }
      }

  def lastWeekTopRating(user: UserId, perf: PerfKey): Fu[IntRating] = lastWeekTopRatingCache.get(user -> perf)

  private val lastWeekTopRatingCache = cacheApi[(UserId, PerfKey), IntRating](1024, "lastWeekTopRating"):
    _.expireAfterAccess(20.minutes).buildAsyncFuture: (userId, perf) =>
      userApi
        .withIntRatingIn(userId, perf)
        .orFail(s"No such user: $userId")
        .flatMap: (user, currentRating) =>
          val firstDay = daysBetween(user.createdAt, nowInstant.minusWeeks(1))
          val days = (firstDay to (firstDay + 6)).toList
          val project = $doc:
            ("_id" -> BSONBoolean(false)) :: days.map: d =>
              s"$perf.$d" -> BSONBoolean(true)
          withColl(_.find($id(user.id), project.some).one[Bdoc].map {
            _.flatMap:
              _.child(perf.value).map {
                _.elements.foldLeft(currentRating):
                  case (max, BSONElement(_, BSONInteger(v))) if max < IntRating(v) => IntRating(v)
                  case (max, _) => max
              }
          }).dmap(_ | currentRating)
