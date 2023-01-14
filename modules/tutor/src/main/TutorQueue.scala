package lila.tutor

import org.joda.time.DateTime
import reactivemongo.api.*
import reactivemongo.api.bson.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import lila.common.IpAddress
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.user.{ User, LightUserApi }
import lila.common.config.Max
import lila.game.Pov

final private class TutorQueue(
    colls: TutorColls,
    gameRepo: lila.game.GameRepo,
    cacheApi: CacheApi,
    lightUserApi: LightUserApi
)(using ExecutionContext, akka.actor.Scheduler):

  import TutorQueue.*

  private val workQueue = lila.hub.AsyncActorSequencer(maxSize = Max(64), timeout = 5 seconds, "tutorQueue")

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
            ~_.flatMap(_.getAsOpt[Int](TutorFullReport.F.millis))
          }
          .map(_.millis)
      }
  }

  def status(user: User): Fu[Status] = workQueue { fetchStatus(user) }

  def enqueue(user: User): Fu[Status] = workQueue {
    colls.queue.insert
      .one($doc(F.id -> user.id, F.requestedAt -> DateTime.now))
      .recover(lila.db.ignoreDuplicateKey)
      .void >> fetchStatus(user)
  }

  private given BSONDocumentReader[Next] = Macros.reader
  def next: Fu[Option[Next]]             = colls.queue.find($empty).sort($sort asc F.requestedAt).one[Next]
  def start(userId: UserId): Funit  = colls.queue.updateField($id(userId), F.startedAt, DateTime.now).void
  def remove(userId: UserId): Funit = colls.queue.delete.one($id(userId)).void

  def waitingGames(user: User): Fu[List[(Pov, PgnStr)]] = for
    all <- gameRepo.recentPovsByUserFromSecondary(user, 60)
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
    colls.queue.primitiveOne[DateTime]($id(user.id), F.requestedAt) flatMap {
      case None => fuccess(NotInQueue)
      case Some(at) =>
        for
          position    <- colls.queue.countSel($doc(F.requestedAt $lte at))
          avgDuration <- durationCache.get({})
        yield InQueue(position, avgDuration)
    }

object TutorQueue:

  sealed trait Status
  case object NotInQueue extends Status
  case class InQueue(position: Int, avgDuration: FiniteDuration) extends Status:
    def eta = avgDuration * position

  private[tutor] case class Next(_id: UserId, startedAt: Option[DateTime]):
    def userId = _id

  object F:
    val id          = "_id"
    val requestedAt = "requestedAt"
    val startedAt   = "startedAt"
