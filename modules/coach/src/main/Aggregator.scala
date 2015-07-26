package lila.coach

import akka.actor._
import akka.pattern.ask
import org.joda.time.DateTime
import play.api.libs.iteratee._
import play.api.libs.json.Json
import reactivemongo.bson._

import lila.db.api._
import lila.db.BSON._
import lila.db.Implicits._
import lila.game.BSONHandlers.gameBSONHandler
import lila.game.tube.gameTube
import lila.game.{ Game, Query }
import lila.hub.Sequencer

final class Aggregator(api: StatApi, sequencer: ActorRef) {

  private implicit val timeout = makeTimeout.minutes(60)

  def apply(userId: String): Funit = {
    val p = scala.concurrent.Promise[Unit]()
    sequencer ? Sequencer.work(compute(userId), p.some)
    p.future
  }

  private def compute(userId: String): Funit = api.fetchLast(userId) flatMap {
    case None    => fromScratch(userId)
    case Some(p) => api.remove(p) >> computeFrom(userId, p.from)
  }

  private def fromScratch(userId: String): Funit =
    pimpQB($query(gameQuery(userId))).sort(Query.sortCreated).skip(maxGames - 1).one[Game] orElse
      pimpQB($query(gameQuery(userId))).sort(Query.sortChronological).one[Game] flatMap {
        _.?? { g => computeFrom(userId, g.createdAt) }
      }

  private def gameQuery(userId: String) = Query.user(userId) ++ Query.rated ++ Query.finished
  private val maxGames = 5 * 1000

  private def computeFrom(userId: String, from: DateTime): Funit = {
    pimpQB($query(gameQuery(userId) ++ Json.obj(Game.BSONFields.createdAt -> $gte($date(from)))))
      .sort(Query.sortChronological)
      .cursor[Game]()
      .enumerate(maxGames, stopOnError = true) &>
      richPovEnumeratee(userId) |>>>
      Iteratee.foldM[Option[RichPov], Periods.Computation](Periods.initComputation(userId, api.insert)) {
        case (comp, Some(p)) => try {
          comp aggregate p
        }
        catch {
          case e: Exception =>
            e.printStackTrace
            logwarn(s"[StatApi] game ${p.pov.game.id} $e"); fuccess(comp)
        }
        // case (comp, Some(p)) => comp aggregate p
        case (comp, _) => logwarn("[StatApi] invalid pov"); fuccess(comp)
      }
  }.map(_.run)

  private def richPovEnumeratee(userId: String) =
    Enumeratee.mapM[lila.game.Game].apply[Option[RichPov]] { game =>
      lila.game.Pov.ofUserId(game, userId) ?? { pov =>
        lila.game.GameRepo.initialFen(game) zip
          (game.metadata.analysed ?? lila.analyse.AnalysisRepo.doneById(game.id)) map {
            case (fen, an) =>
              val division = chess.Replay.boards(
                moveStrs = game.pgnMoves,
                initialFen = fen,
                variant = game.variant
              ).toOption.fold(chess.Division.empty)(chess.Divider.apply)
              RichPov(
                pov = pov,
                initialFen = fen,
                analysis = an,
                division = division,
                accuracy = an.flatMap { lila.analyse.Accuracy(pov, _, division) },
                moveAccuracy = an.map { lila.analyse.Accuracy.diffsList(pov, _) }
              ).some
          }
      }
    }
}
