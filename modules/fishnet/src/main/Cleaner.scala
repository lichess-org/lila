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

  private val moveTimeout = 2 seconds
  private val moveMaxTries = 3

  private def cleanMoves: Funit = moveColl.find(BSONDocument(
    "acquired.date" -> BSONDocument("$lt" -> DateTime.now.minusSeconds(moveTimeout.toSeconds.toInt))
  )).cursor[Work.Move]().collect[List](100).flatMap {
    _.map { move =>
      move.acquiredByKey ?? api.getClient flatMap {
        _ ?? { client =>
          log.warn(s"Timeout move ${move.game.id} by ${client.userId}")
          api.updateMove(move.timeout) zip api.updateClient(client timeout move) void
        }
      }
    }.sequenceFu.void
  } andThenAnyway scheduleMoves

  private def scheduleMoves = scheduler.once(1 second)(cleanMoves)

  scheduler.once(3 seconds)(cleanMoves)
}
