package lila.analyse

import lila.db.dsl._
import lila.game.Game

object AnalysisRepo {

  import Analysis.analysisBSONHandler

  // dirty
  private val coll = Env.current.analysisColl

  type ID = String

  def save(analysis: Analysis) = coll insert analysis void

  def byId(id: ID): Fu[Option[Analysis]] = coll.byId[Analysis](id)

  def byGame(game: Game): Fu[Option[Analysis]] =
    game.metadata.analysed ?? byId(game.id)

  def byIds(ids: Seq[ID]): Fu[Seq[Option[Analysis]]] =
    coll.optionsByOrderedIds[Analysis, Analysis.ID](ids)(_.id)

  def associateToGames(games: List[Game]): Fu[List[Analysis.Analyzed]] =
    byIds(games.map(_.id)) map { as =>
      games zip as collect {
        case (game, Some(analysis)) => Analysis.Analyzed(game, analysis)
      }
    }

  def remove(id: String) = coll remove $id(id)

  def exists(id: String) = coll exists $id(id)
}
