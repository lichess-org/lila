package lila.fishnet

import akka.stream.scaladsl._
import org.joda.time.DateTime
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

final private class Cleaner(
    repo: FishnetRepo,
    moveDb: MoveDB,
    analysisColl: Coll,
    system: akka.actor.ActorSystem
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  import BSONHandlers._

  private def analysisTimeout(plies: Int) = plies * 10.seconds + 5.seconds
  private def analysisTimeoutBase         = analysisTimeout(25)

  private def durationAgo(d: FiniteDuration) = DateTime.now.minusSeconds(d.toSeconds.toInt)

  private def cleanAnalysis: Funit =
    analysisColl.ext
      .find(
        $or(
          $doc("acquired.date" $lt durationAgo(analysisTimeoutBase)),
          $doc("tries" $gte Work.maxTries)
        )
      )
      .sort($sort desc "acquired.date")
      .cursor[Work.Analysis]()
      .documentSource()
      .filter { ana =>
        ana.acquiredAt.fold(true)(_ isBefore durationAgo(analysisTimeout(ana.nbMoves)))
      }
      .take(200)
      .mapAsyncUnordered(4) { ana =>
        repo.updateOrGiveUpAnalysis(ana.timeout) >>-
          logger.info(s"Timeout analysis $ana") >>-
          ana.acquired.foreach { ack =>
            Monitor.timeout(ack.userId)
          }
      }
      .toMat(Sink.ignore)(Keep.right)
      .run()
      .void

  private def cleanMoves: Funit =
    moveDb.clean() map { moves =>
      moves foreach { move =>
        logger.info(s"Timeout move $move")
        move.acquired foreach { ack =>
          Monitor.timeout(ack.userId)
        }
      }
    }

  system.scheduler.scheduleWithFixedDelay(10 seconds, 5 seconds) { () =>
    cleanMoves.unit
  }
  system.scheduler.scheduleWithFixedDelay(15 seconds, 10 seconds) { () =>
    cleanAnalysis.unit
  }
}
