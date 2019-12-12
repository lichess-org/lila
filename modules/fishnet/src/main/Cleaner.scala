package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

private final class Cleaner(
    repo: FishnetRepo,
    analysisColl: Coll,
    monitor: Monitor,
    scheduler: lila.common.Scheduler
) {

  import BSONHandlers._

  private def analysisTimeout(plies: Int) = plies * 6.seconds + 3.seconds
  private def analysisTimeoutBase = analysisTimeout(20)

  private def durationAgo(d: FiniteDuration) = DateTime.now.minusSeconds(d.toSeconds.toInt)

  private def cleanAnalysis: Funit = analysisColl.find(BSONDocument(
    "acquired.date" -> BSONDocument("$lt" -> durationAgo(analysisTimeoutBase))
  )).sort(BSONDocument("acquired.date" -> 1)).cursor[Work.Analysis]().gather[List](100).flatMap {
    _.filter { ana =>
      ana.acquiredAt.??(_ isBefore durationAgo(analysisTimeout(ana.nbMoves)))
    }.map { ana =>
      repo.updateOrGiveUpAnalysis(ana.timeout) >>-
        logger.info(s"Timeout analysis $ana") >>-
        ana.acquired.foreach { ack => Monitor.timeout(ana, ack.userId) }
    }.sequenceFu.void
  }

  scheduler.effect(10 seconds, "fishnet clean analysis")(cleanAnalysis)
}
