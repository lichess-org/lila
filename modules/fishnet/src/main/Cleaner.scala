package lila.fishnet

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final private class Cleaner(
    repo: FishnetRepo,
    analysisColl: Coll,
    system: akka.actor.ActorSystem
)(using Executor, akka.stream.Materializer):

  import BSONHandlers.given

  private def analysisTimeout(plies: Int) = plies * Cleaner.timeoutPerPly + 3.seconds
  private def analysisTimeoutBase = analysisTimeout(20)

  private def durationAgo(d: FiniteDuration) = nowInstant.minusSeconds(d.toSeconds.toInt)

  private def cleanAnalysis: Funit =
    analysisColl
      .find($doc("acquired.date".$lt(durationAgo(analysisTimeoutBase))))
      .sort($sort.desc("acquired.date"))
      .cursor[Work.Analysis]()
      .documentSource()
      .filter: ana =>
        ana.acquiredAt.so(_.isBefore(durationAgo(analysisTimeout(ana.nbMoves))))
      .take(200)
      .mapAsyncUnordered(4): ana =>
        for _ <- repo.updateOrGiveUpAnalysis(ana, _.timeout)
        yield
          logger.info(s"Timeout analysis $ana")
          ana.acquired.foreach: ack =>
            Monitor.timeout(ack.userId)
      .runWith(Sink.ignore)
      .void

  system.scheduler.scheduleWithFixedDelay(15.seconds, 10.seconds): () =>
    cleanAnalysis

object Cleaner:
  val timeoutPerPly = 7.seconds
