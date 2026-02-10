package lila.tutor

import chess.format.pgn.PgnStr
import com.softwaremill.tagging.*
import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.memo.{ CacheApi, SettingStore }

// #TODO use lila.memo.ParallelMongoQueue instead!
final private class TutorQueue(
    colls: TutorColls,
    gameRepo: lila.game.GameRepo,
    cacheApi: CacheApi,
    lightUserApi: lila.core.user.LightUserApi,
    parallelism: SettingStore[Int] @@ Parallelism
)(using Executor, Scheduler):

  import TutorQueue.*

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(64),
    timeout = 5.seconds,
    "tutorQueue",
    lila.log.asyncActorMonitor.full
  )

  private val durationCache = cacheApi.unit[FiniteDuration]:
    _.refreshAfterWrite(1.minutes).buildAsyncFuture: _ =>
      colls.report:
        _.aggregateOne(): framework =>
          import framework.*
          Sort(Descending(TutorFullReport.F.at)) -> List(
            Limit(100),
            Group(BSONNull)(TutorFullReport.F.millis -> AvgField(TutorFullReport.F.millis))
          )
        .map:
          ~_.flatMap(_.getAsOpt[Double](TutorFullReport.F.millis))
        .map(_.toInt.millis)

  def status(user: User): Fu[Status] = workQueue { fetchStatus(user) }

  def enqueue(user: User): Fu[Status] = workQueue:
    colls.queue:
      _.insert
        .one($doc(F.id -> user.id, F.requestedAt -> nowInstant))
        .recover(lila.db.ignoreDuplicateKey)
        .void >> fetchStatus(user)

  def next: Fu[List[Next]] =
    colls.queue(_.find($empty).sort($sort.asc(F.requestedAt)).cursor[Next]().list(parallelism.get()))
  def start(userId: UserId): Funit = colls.queue(_.updateField($id(userId), F.startedAt, nowInstant).void)
  def remove(userId: UserId): Funit = colls.queue(_.delete.one($id(userId)).void)

  def waitingGames(user: User): Fu[List[(Pov, PgnStr)]] = for
    all <- gameRepo.recentPovsByUserFromSecondary(
      user,
      60,
      $doc(lila.core.game.BSONFields.turns.$gt(10))
    )
    (rated, casual) = all.partition(_.game.rated.yes)
    many = rated ::: casual.take(30 - rated.size)
    povs = scalalib.ThreadLocalRandom.shuffle(many).take(30)
    _ <- lightUserApi.preloadMany(povs.flatMap(_.game.userIds))
  yield povs.map { pov =>
    import chess.format.pgn.*
    def playerTag(player: lila.core.game.Player) =
      player.userId.map { uid => Tag(player.color.name, lightUserApi.syncFallback(uid).titleName) }
    val tags = Tags(pov.game.players.flatMap(playerTag))
    pov -> PgnStr(s"$tags\n\n${pov.game.chess.sans.mkString(" ")}")
  }

  private def fetchStatus(user: User): Fu[Status] =
    colls.queue:
      _.primitiveOne[Instant]($id(user.id), F.requestedAt)
        .flatMap:
          _.fold(fuccess(NotInQueue)): at =>
            for
              position <- colls.queue(_.countSel($doc(F.requestedAt.$lte(at))))
              avgDuration <- durationCache.get({})
            yield InQueue(position, avgDuration)

object TutorQueue:

  sealed trait Status
  case object NotInQueue extends Status
  case class InQueue(position: Int, avgDuration: FiniteDuration) extends Status:
    def eta = avgDuration * position

  case class Next(_id: UserId, startedAt: Option[Instant]):
    def userId = _id
  object Next:
    given BSONDocumentReader[Next] = Macros.reader

  object F:
    val id = "_id"
    val requestedAt = "requestedAt"
    val startedAt = "startedAt"
