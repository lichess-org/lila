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
import lila.user.User

final class Aggregator(api: StatApi, sequencer: ActorRef) {

  private implicit val timeout = makeTimeout.minutes(5)

  def apply(user: User): Funit = {
    val p = scala.concurrent.Promise[Unit]()
    sequencer ? Sequencer.work(compute(user), p.some)
    p.future
  }

  private def compute(user: User): Funit = api.fetchLast(user) flatMap {
    case None    => fromScratch(user)
    case Some(p) => api.remove(p) >> computeFrom(user, p.from)
  }

  private def fromScratch(user: User): Funit =
    fetchFirstGame(user) flatMap {
      _.?? { g => computeFrom(user, g.createdAt) }
    }

  private def gameQuery(user: User) = Query.user(user.id) ++ Query.rated ++ Query.finished
  private val maxGames = 5 * 1000

  private def fetchFirstGame(user: User): Fu[Option[Game]] =
    if (user.count.rated == 0) fuccess(none)
    else {
      (user.count.rated >= maxGames) ??
        pimpQB($query(gameQuery(user))).sort(Query.sortCreated).skip(maxGames - 1).one[Game]
    } orElse
      pimpQB($query(gameQuery(user))).sort(Query.sortChronological).one[Game]

  private def computeFrom(user: User, from: DateTime): Funit =
    lila.common.Chronometer(s"aggregator:${user.username}") {
      {
        pimpQB($query(gameQuery(user) ++ Json.obj(Game.BSONFields.createdAt -> $gte($date(from)))))
          .sort(Query.sortChronological)
          .cursor[Game]()
          .enumerate(maxGames, stopOnError = true) &>
          richPovEnumeratee(user) |>>>
          Iteratee.foldM[Option[RichPov], Periods.Computation](Periods.initComputation(user.id, api.insert)) {
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
    }

  private def richPovEnumeratee(user: User) =
    Enumeratee.mapM[lila.game.Game].apply[Option[RichPov]] { game =>
      lila.game.Pov.ofUserId(game, user.id) ?? { pov =>
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
