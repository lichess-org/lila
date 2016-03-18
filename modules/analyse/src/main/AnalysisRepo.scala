package lila.analyse

import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter

import lila.db.api._
import lila.db.Implicits._
import lila.game.Game
import tube.analysisTube

object AnalysisRepo {

  import Analysis.analysisBSONHandler

  type ID = String

  def save(analysis: Analysis) = analysisTube.coll insert analysis void

  def byId(id: ID): Fu[Option[Analysis]] = $find byId id

  def byIds(ids: Seq[ID]): Fu[Seq[Option[Analysis]]] =
    $find optionsByOrderedIds ids

  def associateToGames(games: List[Game]): Fu[List[(Game, Analysis)]] =
    byIds(games.map(_.id)) map { as =>
      games zip as collect {
        case (game, Some(analysis)) => game -> analysis
      }
    }

  def remove(id: String) = $remove byId id

  def exists(id: String) = $count exists id
}
