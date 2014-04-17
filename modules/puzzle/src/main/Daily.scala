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

  private def find: Fu[Option[DailyPuzzle]] = findCurrent orElse findNew flatMap {
    case Some(puzzle) => makeDaily(puzzle)
    case None =>
      scheduler.scheduleOnce(10.seconds)(cache.clear)
      fuccess(none)
  }

  private def makeDaily(puzzle: Puzzle): Fu[Option[DailyPuzzle]] = {
    import chess.format.{ UciMove, Forsyth }
    import makeTimeout.short
    ~(for {
      sit1 <- Forsyth << puzzle.fen
      move <- puzzle.history.lastOption
      uci <- UciMove(move)
      sit2 <- sit1.move(uci.orig, uci.dest, uci.promotion).toOption map (_.situationAfter)
      fen = Forsyth >> sit2
    } yield renderer ? RenderDaily(puzzle, fen, move) map {
      case html: play.api.templates.Html => DailyPuzzle(html, puzzle.color, puzzle.id).some
    })
  } recover {
    case e: Exception =>
      play.api.Logger("daily puzzle").warn(e.getMessage)
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

case class DailyPuzzle(html: play.api.templates.Html, color: chess.Color, id: Int)

case class RenderDaily(puzzle: Puzzle, fen: String, lastMove: String)
