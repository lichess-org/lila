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
  private val sortRecent = BSONDocument("to" -> -1)

  def fetchLast(userId: String): Fu[Option[Period]] =
    coll.find(selectUserId(userId)).sort(sortRecent).one[Period]

  def fetchRange(userId: String, range: Option[Range]): Fu[Option[Period]] =
    range.fold(fetchAll(userId)) { r =>
      coll.find(selectUserId(userId))
        .skip(r.min)
        .sort(sortRecent)
        .cursor[Period]()
        .enumerate(r.size) &>
        Enumeratee.take(r.size) |>>>
        Iteratee.fold[Period, Option[Period]](none) {
          case (a, b) =>
            println(b.data.results.base.nbGames)
            a.fold(b)(_ merge b).some
        }
    }

  def fetchAll(userId: String): Fu[Option[Period]] =
    fetchRange(userId, Range(0, 1000).some)

  def count(userId: String): Fu[Int] =
    coll.count(selectUserId(userId).some)

  def computeIfOld(id: String): Funit = fetchLast(id) flatMap {
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
      import lila.game.tube.gameTube
      import lila.game.BSONHandlers.gameBSONHandler
      import lila.game.Query
      {
        pimpQB($query(Query.user(id) ++ Query.rated ++ Query.finished))
          .sort(Query.sortCreated)
          .cursor[lila.game.Game]()
          .enumerate(10 * 1000, stopOnError = true) &>
          StatApi.richPov(id) |>>>
          Iteratee.fold[Option[RichPov], Periods.Computation](Periods.initComputation(id)) {
            case (comp, Some(p)) => try {
              comp aggregate p
            }
            catch {
              case e: Exception =>
                e.printStackTrace
                logwarn(s"[StatApi] game ${p.pov.game.id} $e"); comp
            }
            // case (comp, Some(p)) => comp aggregate p
            case (comp, _) =>
              logwarn("[StatApi] invalid pov"); comp
          }
      }.map(_.run).flatten("[StatApi] Nothing to persist") flatMap {
        _.periods.list.map { p =>
          coll.insert(p)
        }.sequenceFu.void
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
