package lila.coach

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import reactivemongo.bson.Macros
import reactivemongo.core.commands._
import scala.concurrent.duration._

import lila.db.api.$query
import lila.db.BSON._
import lila.db.Implicits._
import lila.user.UserRepo

final class StatApi(
    coll: Coll,
    makeThrottler: (String => Funit) => Throttler) {

  import BSONHandlers._

  private def selectUserId(id: String) = BSONDocument("userId" -> id)
  private val sortChronological = BSONDocument("from" -> 1)

  def fetchRange(userId: String, range: Option[Range]): Fu[Option[Period]] =
    range.fold(fetchAll(userId)) { r =>
      coll.find(selectUserId(userId))
        .skip(r.min)
        .sort(sortChronological)
        .cursor[Period]()
        .enumerate(r.size) &>
        Enumeratee.take(r.size) |>>>
        Iteratee.fold[Period, Option[Period]](none) {
          case (a, b) => a.fold(b)(_ merge b).some
        }
    }

  def fetchAll(userId: String): Fu[Option[Period]] =
    fetchRange(userId, Range(0, 1000).some)

  def fetchFirst(userId: String): Fu[Option[Period]] =
    fetchRange(userId, Range(0, 1).some)

  def count(userId: String): Fu[Int] =
    coll.count(selectUserId(userId).some)

  def computeIfOld(id: String): Funit = fetchFirst(id) flatMap {
    case Some(stat) => funit
    case _          => throttler(id)
  }

  def computeForce(id: String): Funit = throttler(id)

  private val throttler = makeThrottler { id =>
    def aggregate(period: Period.Computation, povOption: Option[RichPov], gameId: String) = povOption match {
      case Some(pov) => try {
        period aggregate pov
      }
      catch {
        case e: Exception => logwarn("[StatApi] " + e); period
      }
      case _ => logwarn("[StatApi] invalid game " + gameId); period
    }
    coll.remove(selectUserId(id)) >> {
      import play.api.libs.json.Json
      import lila.game.tube.gameTube
      import lila.game.BSONHandlers.gameBSONHandler
      import lila.game.{ Game, Query }
      import lila.db.api._
      val gameQuery = Query.user(id) ++ Query.rated ++ Query.finished
      val maxGames = 5 * 1000
      pimpQB($query(gameQuery)).sort(Query.sortCreated).skip(maxGames - 1).one[Game] flatMap {
        _.?? { firstGame =>
          {
            pimpQB($query(gameQuery ++ Json.obj(Game.BSONFields.createdAt -> $gte($date(firstGame.createdAt)))))
              .sort(Query.sortChronological)
              .cursor[Game]()
              .enumerate(maxGames, stopOnError = true) &>
              StatApi.richPov(id) |>>>
              Iteratee.foldM[Option[RichPov], Periods.Computation](Periods.initComputation(id, { p => coll.insert(p).void })) {
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
      }
    }
  }
}

private object StatApi {

  def richPov(userId: String) = Enumeratee.mapM[lila.game.Game].apply[Option[RichPov]] { game =>
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
