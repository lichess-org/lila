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

  private implicit val openingsHandler = MapValue.MapHandler[Int]
  import UserStat.Openings
  private implicit val UserStatOpeningsBSONHandler = Macros.handler[Openings]
  private implicit val UserStatBSONHandler = Macros.handler[UserStat]

  private def selectId(id: String) = BSONDocument("_id" -> id)

  def fetch(id: String): Fu[Option[UserStat]] = coll.find(selectId(id)).one[UserStat]

  def computeIfOld(id: String): Fu[Option[UserStat]] = fetch(id) flatMap {
    case Some(stat) if stat.isFresh => fuccess(stat.some)
    case _                          => compute(id)
  }

  private def compute(id: String): Fu[Option[UserStat]] = {
    import lila.game.tube.gameTube
    import lila.game.BSONHandlers.gameBSONHandler
    import lila.game.Query
    pimpQB($query(Query.user(id) ++ Query.rated))
      .sort(Query.sortCreated)
      .cursor[lila.game.Game]().enumerate(10 * 1000, stopOnError = false) &>
      StatApi.withAnalysis |>>>
      Iteratee.fold(UserStat(id)) {
        case (stat, a) => lila.game.Pov.ofUserId(a.game, id).fold(stat) { stat.withGame(_, a.analysis) }
      }
  } flatMap { stat =>
    (stat.nbGames > 0) ?? (coll.update(selectId(id), stat, upsert = true) inject stat.some)
  }
}

private object StatApi {

  val withAnalysis = Enumeratee.mapM[lila.game.Game].apply[Analysed] { game =>
    import lila.analyse.AnalysisRepo
    (game.metadata.analysed ?? AnalysisRepo.doneById(game.id)) map { Analysed(game, _) }
  }
  case class Analysed(game: lila.game.Game, analysis: Option[lila.analyse.Analysis])
}
