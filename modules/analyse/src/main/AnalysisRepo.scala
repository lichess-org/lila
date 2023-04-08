package lila.analyse

import lila.db.dsl.*
import lila.game.Game

final class AnalysisRepo(val coll: Coll)(using Executor):

  import AnalyseBsonHandlers.given

  type ID = String

  def save(analysis: Analysis) = coll.insert one analysis void

  def byId(id: ID): Fu[Option[Analysis]] = coll.byId[Analysis](id)

  def byGame(game: Game): Fu[Option[Analysis]] =
    game.metadata.analysed ?? byId(game.id.value)

  def byIds(ids: Seq[ID]): Fu[Seq[Option[Analysis]]] =
    coll.optionsByOrderedIds[Analysis, Analysis.ID](ids)(_.id)

  def associateToGames(games: List[Game]): Fu[List[(Game, Analysis)]] =
    byIds(games.map(_.id.value)) map { as =>
      games zip as collect { case (game, Some(analysis)) =>
        game -> analysis
      }
    }

  def remove(id: String) = coll.delete one $id(id)

  def exists(id: String) = coll exists $id(id)
