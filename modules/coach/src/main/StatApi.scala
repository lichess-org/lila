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
    makeThrottler: (String => Fu[UserStat]) => Throttler) {

  import BSONHandlers._

  private def selectId(id: String) = BSONDocument("_id" -> id)

  def fetch(id: String): Fu[Option[UserStat]] = coll.find(selectId(id)).one[UserStat]

  def fetchOrCompute(id: String): Fu[UserStat] = fetch(id) flatMap {
    case Some(s) => fuccess(s)
    case None    => compute(id)
  }

  def computeIfOld(id: String): Fu[UserStat] = fetch(id) flatMap {
    case Some(stat) if stat.isFresh => fuccess(stat)
    case _                          => compute(id)
  }

  def computeForce(id: String): Fu[UserStat] = compute(id)

  private val throttler = makeThrottler { id =>
    import lila.game.tube.gameTube
    import lila.game.BSONHandlers.gameBSONHandler
    import lila.game.Query
    pimpQB($query(Query.user(id) ++ Query.rated ++ Query.finished))
      .sort(Query.sortCreated)
      .cursor[lila.game.Game]()
      .enumerate(10 * 1000, stopOnError = true) &>
      StatApi.richPov(id) |>>>
      Iteratee.fold[Option[RichPov], UserStat.Computation](UserStat.makeComputation(id)) {
        case (comp, Some(pov)) => try {
          comp aggregate pov
        }
        catch {
          case e: Exception => logwarn("[StatApi] " + e); comp
        }
        case (comp, _) => comp
      } map (_.run) flatMap { stat =>
        coll.update(selectId(id), stat, upsert = true) inject stat
      }
  }

  private def compute(id: String): Fu[UserStat] = throttler(id)
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
