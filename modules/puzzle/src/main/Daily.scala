package lila.puzzle

import scala.concurrent.duration._

import akka.actor.{ ActorSelection, Scheduler }
import akka.pattern.ask
import org.joda.time.DateTime

import lila.db.dsl._

private[puzzle] final class Daily(
    coll: Coll,
    renderer: ActorSelection,
    scheduler: Scheduler) {

  private val cache =
    lila.memo.AsyncCache.single[Option[DailyPuzzle]](
      name = "puzzle.daily",
      f = find,
      timeToLive = 10 minutes)

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
      renderer ? RenderDaily(puzzle, fen, puzzle.initialMove.uci) map {
        case html: play.twirl.api.Html => DailyPuzzle(html, puzzle.color, puzzle.id).some
      }
    }
  } recover {
    case e: Exception =>
      logger.warn("make daily", e)
      none
  }

  private def findCurrent = coll.find(
    $doc("day" $gt DateTime.now.minusMinutes(24 * 60 - 15))
  ).uno[Puzzle]

  private def findNew = coll.find(
    $doc("day" $exists false)
  ).sort($doc("vote.sum" -> -1)).uno[Puzzle] flatMap {
      case Some(puzzle) => coll.update(
        $id(puzzle.id),
        $set("day" -> DateTime.now)
      ) inject puzzle.some
      case None => fuccess(none)
    }
}

case class DailyPuzzle(html: play.twirl.api.Html, color: chess.Color, id: Int)

case class RenderDaily(puzzle: Puzzle, fen: String, lastMove: String)
