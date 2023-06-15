package lila.analyse

import lila.db.dsl.{ given, * }
import lila.game.Game

final class AnalysisRepo(val coll: Coll)(using Executor):

  import AnalyseBsonHandlers.given

  def save(analysis: Analysis) = coll.insert one analysis void

  def byId(id: Analysis.Id): Fu[Option[Analysis]] = coll.byId[Analysis](id)

  def byGame(game: Game): Fu[Option[Analysis]] =
    game.metadata.analysed so byId(game.id into Analysis.Id)

  def byIds(ids: Seq[Analysis.Id]): Fu[Seq[Option[Analysis]]] =
    coll.optionsByOrderedIds[Analysis, Analysis.Id](ids)(_.id)

  def associateToGames(games: List[Game]): Fu[List[(Game, Analysis)]] =
    byIds(games.map(_.id into Analysis.Id)).map: as =>
      games zip as collect { case (game, Some(analysis)) =>
        game -> analysis
      }

  def remove(id: String) = coll.delete one $id(id)

  def exists(id: String) = coll exists $id(id)
