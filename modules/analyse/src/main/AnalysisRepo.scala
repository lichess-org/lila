package lila.analyse

import org.joda.time.DateTime
import play.api.libs.json.Json

import lila.db.dsl._
import lila.game.Game

object AnalysisRepo {

  import Analysis.analysisBSONHandler

  // dirty
  private val coll = Env.current.analysisColl

  type ID = String

  def save(analysis: Analysis) = coll insert analysis void

  def byId(id: ID): Fu[Option[Analysis]] = coll.byId[Analysis](id)

  def byIds(ids: Seq[ID]): Fu[Seq[Option[Analysis]]] =
    coll.optionsByOrderedIds[Analysis](ids)(_.id)

  def associateToGames(games: List[Game]): Fu[List[(Game, Analysis)]] =
    byIds(games.map(_.id)) map { as =>
      games zip as collect {
        case (game, Some(analysis)) => game -> analysis
      }
    }

  def remove(id: String) = coll remove $id(id)

  def exists(id: String) = coll exists $id(id)
}
