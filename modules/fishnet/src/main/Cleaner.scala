package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._

private final class Cleaner(
    api: FishnetApi,
    moveColl: Coll,
    analysisColl: Coll,
    scheduler: lila.common.Scheduler) {

  import BSONHandlers._

  private val moveTimeout = 2.seconds
  private def analysisTimeout(plies: Int) = plies * 6.seconds + 10.seconds
  private def analysisTimeoutBase = analysisTimeout(20)

  private def durationAgo(d: FiniteDuration) = DateTime.now.minusSeconds(d.toSeconds.toInt)

  private def cleanMoves: Funit = moveColl.find(BSONDocument(
    "acquired.date" -> BSONDocument("$lt" -> durationAgo(moveTimeout))
  )).sort(BSONDocument("acquired.date" -> 1)).cursor[Work.Move]().collect[List](100).flatMap {
    _.map { move =>
      api.repo.updateOrGiveUpMove(move) zip {
        move.acquiredByKey ?? api.repo.getClient flatMap {
          _ ?? { client =>
            api.repo.updateOrGiveUpMove(move) zip
              api.repo.updateClient(client timeout move) >>-
              log.warn(s"Timeout client ${client.fullId}")
          }
        }
      } >>- log.warn(s"Timeout move ${move.game.id}")
    }.sequenceFu.void
  } andThenAnyway scheduleMoves

  private def cleanAnalysis: Funit = analysisColl.find(BSONDocument(
    "acquired.date" -> BSONDocument("$lt" -> durationAgo(analysisTimeoutBase))
  )).sort(BSONDocument("acquired.date" -> 1)).cursor[Work.Analysis]().collect[List](100).flatMap {
    _.filter { ana =>
      ana.acquiredAt.??(_ isBefore durationAgo(analysisTimeout(ana.nbPly)))
    }.map { ana =>
      api.repo.updateOrGiveUpAnalysis(ana) zip {
        ana.acquiredByKey ?? api.repo.getClient flatMap {
          _ ?? { client =>
            api.repo.updateClient(client timeout ana) >>-
              log.warn(s"Timeout client ${client.fullId}")
          }
        }
      } >>- log.warn(s"Timeout analysis ${ana.game.id}")
    }.sequenceFu.void
  } andThenAnyway scheduleAnalysis

  private def scheduleMoves = scheduler.once(1 second)(cleanMoves)
  private def scheduleAnalysis = scheduler.once(5 second)(cleanAnalysis)

  scheduler.once(3 seconds)(cleanMoves)
  scheduler.once(10 seconds)(cleanAnalysis)
}
