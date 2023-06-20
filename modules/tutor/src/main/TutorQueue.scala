package lila.tutor

import reactivemongo.api.*
import reactivemongo.api.bson.*
import com.softwaremill.tagging.*

import lila.db.dsl.{ *, given }
import lila.user.{ User, LightUserApi }
import lila.common.config.Max
import lila.game.Pov
import lila.memo.{ SettingStore, CacheApi }
import chess.format.pgn.PgnStr

final private class TutorQueue(
    colls: TutorColls,
    gameRepo: lila.game.GameRepo,
    cacheApi: CacheApi,
    lightUserApi: LightUserApi,
    parallelism: SettingStore[Int] @@ Parallelism
)(using Executor, Scheduler):

  import TutorQueue.*

  private val workQueue = lila.hub.AsyncActorSequencer(maxSize = Max(64), timeout = 5.seconds, "tutorQueue")

  private val durationCache = cacheApi.unit[FiniteDuration] {
    _.refreshAfterWrite(1 minutes)
      .buildAsyncFuture { _ =>
        colls.report
          .aggregateOne(ReadPreference.secondaryPreferred) { framework =>
            import framework.*
            Sort(Descending(TutorFullReport.F.at)) -> List(
              Limit(100),
              Group(BSONNull)(TutorFullReport.F.millis -> AvgField(TutorFullReport.F.millis))
            )
          }
          .map {
            ~_.flatMap(_.getAsOpt[Double](TutorFullReport.F.millis))
          }
          .map(_.toInt.millis)
      }
  }

  def status(user: User): Fu[Status] = workQueue { fetchStatus(user) }

  def enqueue(user: User): Fu[Status] = workQueue {
    colls.queue.insert
      .one($doc(F.id -> user.id, F.requestedAt -> nowInstant))
      .recover(lila.db.ignoreDuplicateKey)
      .void >> fetchStatus(user)
  }

  def next: Fu[List[Next]] =
    colls.queue.find($empty).sort($sort asc F.requestedAt).cursor[Next]().list(parallelism.get())
  def start(userId: UserId): Funit  = colls.queue.updateField($id(userId), F.startedAt, nowInstant).void
  def remove(userId: UserId): Funit = colls.queue.delete.one($id(userId)).void

  def waitingGames(user: User): Fu[List[(Pov, PgnStr)]] = for
    all <- gameRepo.recentPovsByUserFromSecondary(user, 60, $doc(lila.game.Game.BSONFields.turns $gt 10))
    (rated, casual) = all.partition(_.game.rated)
    many            = rated ::: casual.take(30 - rated.size)
    povs            = ornicar.scalalib.ThreadLocalRandom.shuffle(many).take(30)
    _ <- lightUserApi.preloadMany(povs.flatMap(_.game.userIds))
  yield povs map { pov =>
    import chess.format.pgn.*
    def playerTag(player: lila.game.Player) =
      player.userId.map { uid => Tag(player.color.name, lightUserApi.syncFallback(uid).titleName) }
    val tags = Tags(pov.game.players.flatMap(playerTag))
    pov -> PgnStr(s"$tags\n\n${pov.game.chess.sans.mkString(" ")}")
  }

  private def fetchStatus(user: User): Fu[Status] =
    colls.queue.primitiveOne[Instant]($id(user.id), F.requestedAt) flatMap {
      _.fold(fuccess(NotInQueue)) { at =>
        for
          position    <- colls.queue.countSel($doc(F.requestedAt $lte at))
          avgDuration <- durationCache.get({})
        yield InQueue(position, avgDuration)
      }
    }

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
    val id          = "_id"
    val requestedAt = "requestedAt"
    val startedAt   = "startedAt"
