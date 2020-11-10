package lila.puzzle

import akka.pattern.ask
import org.joda.time.DateTime
import Puzzle.{ BSONFields => F }
import scala.concurrent.duration._

import lila.db.AsyncColl
import lila.db.dsl._
import lila.memo.CacheApi._

final private[puzzle] class Daily(
    coll: AsyncColl,
    renderer: lila.hub.actors.Renderer,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  private val cache =
    cacheApi.unit[Option[DailyPuzzle]] {
      _.refreshAfterWrite(30 minutes)
        .buildAsyncFuture(_ => find)
    }

  def get: Fu[Option[DailyPuzzle]] = cache.getUnit

  private def find: Fu[Option[DailyPuzzle]] =
    (findCurrent orElse findNew) recover { case e: Exception =>
      logger.error("find daily", e)
      none
    } flatMap {
      case Some(puzzle) => makeDaily(puzzle)
      case None         => fuccess(none)
    }

  private def makeDaily(puzzle: Puzzle): Fu[Option[DailyPuzzle]] = {
    import makeTimeout.short
    renderer.actor ? RenderDaily(puzzle, puzzle.fenAfterInitialMove, puzzle.line.head.uci) map {
      case html: String => DailyPuzzle(html, puzzle.color, puzzle.id).some
    }
  } recover { case e: Exception =>
    logger.warn("make daily", e)
    none
  }

  private def findCurrent =
    coll {
      _.find(
        $doc(F.day $gt DateTime.now.minusMinutes(24 * 60 - 15))
      )
        .one[Puzzle]
    }

  private def findNew =
    coll { c =>
      c.find($doc(F.day $exists false))
        .sort($doc(F.vote -> -1))
        .one[Puzzle] flatMap {
        case Some(puzzle) =>
          c.update.one(
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

case class DailyPuzzle(html: String, color: chess.Color, id: Puzzle.Id)

case class RenderDaily(puzzle: Puzzle, fen: chess.format.FEN, lastMove: String)
