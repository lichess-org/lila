package lila.tutor

import org.joda.time.DateTime
import reactivemongo.api.*
import reactivemongo.api.bson.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import lila.common.IpAddress
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.user.User
import lila.common.config.Max

final private class TutorQueue(
    colls: TutorColls,
    gameRepo: lila.game.GameRepo,
    cacheApi: CacheApi
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

  def waitingGames(user: User): Fu[List[PgnStr]] = gameRepo.recentPovsByUserFromSecondary(user, 10) map {
    _.map { pov =>
      PgnStr(pov.game.chess.sans.mkString(" "))
    }
  }

  private def fetchStatus(user: User): Fu[Status] =
    fuccess(InQueue(1, 5.minutes))
    // queueColl.primitiveOne[DateTime]($id(user.id), F.requestedAt) flatMap {
    //   case None => fuccess(NotInQueue)
    //   case Some(at) =>
    //     for {
    //       position    <- queueColl.countSel($doc(F.requestedAt $lte at))
    //       avgDuration <- durationCache.get({})
    //     } yield InQueue(position, avgDuration)
    // }

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
