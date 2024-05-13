package lila.analyse

import lila.db.dsl.*
import lila.tree.Analysis

final class AnalysisRepo(val coll: Coll)(using Executor):

  import AnalyseBsonHandlers.given

  def save(analysis: Analysis) = coll.insert.one(analysis) void

  def byId(id: Analysis.Id): Fu[Option[Analysis]] = coll.byId[Analysis](id.value)

  def byGame(game: Game): Fu[Option[Analysis]] =
    game.metadata.analysed.so(byId(Analysis.Id(game.id)))

  def byIds(ids: Seq[Analysis.Id]): Fu[Seq[Option[Analysis]]] =
    coll.optionsByOrderedIds[Analysis, String](ids.map(_.value))(_.id.value)

  def associateToGames(games: List[Game]): Fu[List[(Game, Analysis)]] =
    byIds(games.map(g => Analysis.Id(g.id))).map: as =>
      games.zip(as).collect { case (game, Some(analysis)) =>
        game -> analysis
      }

  def remove(id: GameId) = coll.delete.one($id(Analysis.Id(id).value))

  def exists(id: GameId)                = coll.exists($id(id.value))
  def chapterExists(id: StudyChapterId) = coll.exists($id(id.value))
