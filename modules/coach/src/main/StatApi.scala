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

final class StatApi(coll: Coll) {

  import BSONHandlers._

  private def selectId(id: String) = BSONDocument("_id" -> id)

  def fetch(id: String): Fu[Option[UserStat]] = coll.find(selectId(id)).one[UserStat]

  def computeIfOld(id: String): Fu[UserStat] = fetch(id) flatMap {
    case Some(stat) if stat.isFresh => fuccess(stat)
    case _                          => compute(id)
  }

  private def compute(id: String): Fu[UserStat] = {
    import lila.game.tube.gameTube
    import lila.game.BSONHandlers.gameBSONHandler
    import lila.game.Query
    pimpQB($query(Query.user(id) ++ Query.rated ++ Query.finished))
      .sort(Query.sortCreated)
      .cursor[lila.game.Game]().enumerate(10 * 1000, stopOnError = false) &>
      StatApi.withAnalysis |>>>
      Iteratee.fold(UserStat.makeComputation(id)) {
        case (comp, a) => lila.game.Pov.ofUserId(a.game, id).fold(comp) {
          comp.aggregate(_, a.analysis)
        }
      }
  } map (_.run) flatMap { stat =>
    coll.update(selectId(id), stat, upsert = true) inject stat
  }
}

private object StatApi {

  val withAnalysis = Enumeratee.mapM[lila.game.Game].apply[Analysed] { game =>
    import lila.analyse.AnalysisRepo
    (game.metadata.analysed ?? AnalysisRepo.doneById(game.id)) map { Analysed(game, _) }
  }
  case class Analysed(game: lila.game.Game, analysis: Option[lila.analyse.Analysis])
}
