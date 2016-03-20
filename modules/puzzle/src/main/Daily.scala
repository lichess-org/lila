package lila.puzzle

import scala.concurrent.duration._

import akka.actor.{ ActorSelection, Scheduler }
import akka.pattern.ask
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll

private[puzzle] final class Daily(
    coll: Coll,
    renderer: ActorSelection,
    scheduler: Scheduler) {

  private val cache =
    lila.memo.AsyncCache.single[Option[DailyPuzzle]](f = find, timeToLive = 30 minutes)

  def apply(): Fu[Option[DailyPuzzle]] = cache apply true

  private def find: Fu[Option[DailyPuzzle]] = (findCurrent orElse findNew) recover {
    case e: Exception =>
      logger.error("find daily", e)
      none
  } flatMap {
    case Some(puzzle) => makeDaily(puzzle)
    case None =>
      scheduler.scheduleOnce(10.seconds)(cache.clear)
      fuccess(none)
  }

  private def makeDaily(puzzle: Puzzle): Fu[Option[DailyPuzzle]] = {
    import makeTimeout.short
    ~puzzle.fenAfterInitialMove.map { fen =>
      renderer ? RenderDaily(puzzle, fen, puzzle.initialMove) map {
        case html: play.twirl.api.Html => DailyPuzzle(html, puzzle.color, puzzle.id).some
      }
    }
  } recover {
    case e: Exception =>
      logger.warn("make daily", e)
      none
  }

  private def findCurrent = coll.find(
    BSONDocument("day" -> BSONDocument("$gt" -> DateTime.now.minusMinutes(24 * 60 - 15)))
  ).one[Puzzle]

  private def findNew = coll.find(
    BSONDocument("day" -> BSONDocument("$exists" -> false))
  ).sort(BSONDocument("vote.sum" -> -1)).one[Puzzle] flatMap {
      case Some(puzzle) => coll.update(
        BSONDocument("_id" -> puzzle.id),
        BSONDocument("$set" -> BSONDocument("day" -> DateTime.now))
      ) inject puzzle.some
      case None => fuccess(none)
    }
}

case class DailyPuzzle(html: play.twirl.api.Html, color: chess.Color, id: Int)

case class RenderDaily(puzzle: Puzzle, fen: String, lastMove: String)
