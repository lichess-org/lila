package lila.puzzle

import scala.concurrent.duration._

import akka.pattern.ask
import org.joda.time.DateTime

import lila.db.AsyncColl
import lila.db.dsl._
import Puzzle.{ BSONFields => F }

private[puzzle] final class Daily(
    coll: AsyncColl,
    renderer: lila.hub.actors.Renderer,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val cache =
    asyncCache.single[Option[DailyPuzzle]](
      name = "puzzle.daily",
      f = find,
      expireAfter = _.ExpireAfterWrite(10 minutes)
    )

  def get: Fu[Option[DailyPuzzle]] = cache.get

  private def find: Fu[Option[DailyPuzzle]] = (findCurrent orElse findNew) recover {
    case e: Exception =>
      logger.error("find daily", e)
      none
  } flatMap {
    case Some(puzzle) => makeDaily(puzzle)
    case None => fuccess(none)
  }

  private def makeDaily(puzzle: Puzzle): Fu[Option[DailyPuzzle]] = {
    import makeTimeout.short
    ~puzzle.fenAfterInitialMove.map { fen =>
      renderer.actor ? RenderDaily(puzzle, fen, puzzle.initialMove.uci) map {
        case html: String => DailyPuzzle(html, puzzle.color, puzzle.id).some
      }
    }
  } recover {
    case e: Exception =>
      logger.warn("make daily", e)
      none
  }

  private def findCurrent = coll {
    _.ext.find(
      $doc(F.day $gt DateTime.now.minusMinutes(24 * 60 - 15))
    ).one[Puzzle]
  }

  private def findNew = coll { c =>
    c.ext.find(
      $doc(F.day $exists false, F.voteNb $gte 200)
    ).sort($doc(F.voteRatio -> -1)).one[Puzzle] flatMap {
        case Some(puzzle) => c.update.one(
          $id(puzzle.id),
          $set(F.day -> DateTime.now)
        ) inject puzzle.some
        case None => fuccess(none)
      }
  }
}

object Daily {
  type Try = () => Fu[Option[DailyPuzzle]]
}

case class DailyPuzzle(html: String, color: chess.Color, id: Int)

case class RenderDaily(puzzle: Puzzle, fen: String, lastMove: String)
