package lila.tutor

import reactivemongo.api.*
import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key
import com.softwaremill.tagging.*

import lila.db.dsl.{ *, given }
import lila.user.{ User, LightUserApi }
import lila.common.config.Max
import lila.game.Pov
import lila.memo.{ SettingStore, CacheApi }
import lila.tutor.TutorPeriodReport.Query
import chess.format.pgn.PgnStr

final private class TutorQueue(
    colls: TutorColls,
    gameRepo: lila.game.GameRepo,
    cacheApi: CacheApi,
    lightUserApi: LightUserApi,
    parallelism: SettingStore[Int] @@ Parallelism
)(using Executor, Scheduler):

  import TutorQueue.*
  import TutorBsonHandlers.{ *, given }

  private val workQueue = lila.hub.AsyncActorSequencer(maxSize = Max(64), timeout = 5.seconds, "tutorQueue")

  private val durationCache = cacheApi.unit[FiniteDuration]:
    _.refreshAfterWrite(1 minutes).buildAsyncFuture: _ =>
      colls.report
        .aggregateOne(_.sec): framework =>
          import framework.*
          Sort(Descending(TutorPeriodReport.F.at)) -> List(
            Limit(100),
            Group(BSONNull)(TutorPeriodReport.F.millis -> AvgField(TutorPeriodReport.F.millis))
          )
        .map:
          ~_.flatMap(_.getAsOpt[Double](TutorFullReport.F.millis))
        .map(_.toInt.millis)

  def enqueue(query: Query): Fu[Boolean] = workQueue:
    colls.queue.insert
      .one(Queued(query))
      .inject(true)
      .recover(lila.db.recoverDuplicateKey(_ => false))

  def next: Fu[List[Queued]] =
    colls.queue.find($empty).sort($sort asc F.requestedAt).cursor[Queued]().list(parallelism.get())
  def start(userId: UserId): Funit  = colls.queue.updateField($id(userId), F.startedAt, nowInstant).void
  def remove(userId: UserId): Funit = colls.queue.delete.one($id(userId)).void

  def waitingGames(user: User): Fu[List[(Pov, PgnStr)]] = for
    all <- gameRepo.recentPovsByUserFromSecondary(user, 60, $doc(lila.game.Game.BSONFields.turns $gt 10))
    (rated, casual) = all.partition(_.game.rated)
    many            = rated ::: casual.take(30 - rated.size)
    povs            = ornicar.scalalib.ThreadLocalRandom.shuffle(many).take(30)
    _ <- lightUserApi.preloadMany(povs.flatMap(_.game.userIds))
  yield povs.map: pov =>
    import chess.format.pgn.*
    def playerTag(player: lila.game.Player) =
      player.userId.map { uid => Tag(player.color.name, lightUserApi.syncFallback(uid).titleName) }
    val tags = Tags(pov.game.players.flatMap(playerTag))
    pov -> PgnStr(s"$tags\n\n${pov.game.chess.sans.mkString(" ")}")

  def fetchStatus(user: User): Fu[Option[InQueue]] =
    colls.queue.one[Queued]($id(user.id)) flatMap {
      _.so: q =>
        for
          position    <- colls.queue.countSel($doc(F.requestedAt $lte q.requestedAt))
          avgDuration <- durationCache.get({})
        yield InQueue(q.query, position, avgDuration).some
    }

object TutorQueue:

  case class Queued(
      @Key("_id") userId: UserId,
      query: Query,
      requestedAt: Instant,
      startedAt: Option[Instant]
  )
  object Queued:
    def apply(query: Query): Queued = Queued(query.user, query, nowInstant, none)
  object F:
    val id          = "_id"
    val query       = "query"
    val requestedAt = "requestedAt"
    val startedAt   = "startedAt"

  case class InQueue(query: Query, position: Int, avgDuration: FiniteDuration):
    export query.*
    def eta = avgDuration * position
