package lila.fishnet

import akka.stream.scaladsl._
import org.joda.time.DateTime
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

private final class Cleaner(
    repo: FishnetRepo,
    analysisColl: Coll,
    system: akka.actor.ActorSystem
)(implicit mat: akka.stream.Materializer) {

  import BSONHandlers._

  private def analysisTimeout(plies: Int) = plies * 6.seconds + 3.seconds
  private def analysisTimeoutBase = analysisTimeout(20)

  private def durationAgo(d: FiniteDuration) = DateTime.now.minusSeconds(d.toSeconds.toInt)

  private def cleanAnalysis: Funit = analysisColl.ext
    .find($doc("acquired.date" $lt durationAgo(analysisTimeoutBase)))
    .sort($sort desc "acquired.date")
    .cursor[Work.Analysis]()
    .documentSource()
    .filter { ana =>
      ana.acquiredAt.??(_ isBefore durationAgo(analysisTimeout(ana.nbMoves)))
    }
    .take(200)
    .mapAsyncUnordered(4) { ana =>
      repo.updateOrGiveUpAnalysis(ana.timeout) >>-
        logger.info(s"Timeout analysis $ana") >>-
        ana.acquired.foreach { ack => Monitor.timeout(ana, ack.userId) }
    }
    .toMat(Sink.ignore)(Keep.right)
    .run
    .void

  system.scheduler.scheduleWithFixedDelay(15 seconds, 10 seconds) { () => cleanAnalysis }
}
