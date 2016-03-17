package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._

private final class Cleaner(
    repo: FishnetRepo,
    moveDb: MoveDB,
    analysisColl: Coll,
    monitor: Monitor,
    scheduler: lila.common.Scheduler) {

  import BSONHandlers._

  private val moveTimeout = 2.seconds
  private def analysisTimeout(plies: Int) = plies * 6.seconds + 3.seconds
  private def analysisTimeoutBase = analysisTimeout(20)

  private def durationAgo(d: FiniteDuration) = DateTime.now.minusSeconds(d.toSeconds.toInt)

  private def cleanMoves: Unit = {
    val since = durationAgo(moveTimeout)
    moveDb.find(_ acquiredBefore since).map { move =>
      moveDb updateOrGiveUp move.timeout
      clientTimeout(move)
      log.warn(s"Timeout move ${move.game.id}")
    }
    scheduleMoves
  }

  private def cleanAnalysis: Funit = analysisColl.find(BSONDocument(
    "acquired.date" -> BSONDocument("$lt" -> durationAgo(analysisTimeoutBase))
  )).sort(BSONDocument("acquired.date" -> 1)).cursor[Work.Analysis]().collect[List](100).flatMap {
    _.filter { ana =>
      ana.acquiredAt.??(_ isBefore durationAgo(analysisTimeout(ana.nbPly)))
    }.map { ana =>
      repo.updateOrGiveUpAnalysis(ana.timeout) >>- {
        clientTimeout(ana)
        log.warn(s"Timeout analysis ${ana.game.id}")
      }
    }.sequenceFu.void
  } andThenAnyway scheduleAnalysis

  private def clientTimeout(work: Work) = work.acquiredByKey ?? repo.getClient foreach {
    _ foreach { client =>
      monitor.timeout(work, client)
      log.warn(s"Timeout client ${client.fullId}")
    }
  }

  private def scheduleMoves = scheduler.once(1 second)(cleanMoves)
  private def scheduleAnalysis = scheduler.once(5 second)(cleanAnalysis)

  scheduler.once(3 seconds)(cleanMoves)
  scheduler.once(10 seconds)(cleanAnalysis)
}
