package lila.evaluation

import play.api.libs.json.Json


import lila.db.api._
import lila.db.Implicits._
import lila.analyse.{ Analysis, AnalysisRepo }
import lila.game.Game
import lila.game.Game.{ BSONFields => G }
import lila.game.Query
import lila.game.tube.gameTube

object GamePool {

  case class Analysed(game: Game, analysis: Option[Analysis])

  def analysedGamesOf(userId: String, nb: Int): Fu[List[Analysed]] =
    gamesOf(userId, nb) flatMap { games =>
      AnalysisRepo doneByIds games.map(_.id) map { analysisOptions =>
        games zip analysisOptions map {
          case (game, analysis) => Analysed(game, analysis)
        }
      }
    }

  private def gamesOf(userId: String, nb: Int) =
    $find($query(
      Query.finished ++
      Query.user(userId) ++
      Query.rated ++
      Json.obj(G.variant -> $nin(Game.unanalysableVariants.map(_.id)))
    ) sort Query.sortCreated, nb)
}
