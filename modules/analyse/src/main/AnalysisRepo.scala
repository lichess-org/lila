package lila.analyse

import lila.db.dsl.*
import lila.tree.Analysis
import reactivemongo.api.bson.*

final class AnalysisRepo(val coll: Coll)(using Executor):

  import AnalyseBsonHandlers.given

  def byId(id: Analysis.Id): Fu[Option[Analysis]] = coll.secondary.byId[Analysis](id)

  def byGame(game: Game): Fu[Option[Analysis]] =
    game.metadata.analysed.so(byId(Analysis.Id(game.id)))

  def byIds(ids: Seq[Analysis.Id]): Fu[Seq[Option[Analysis]]] =
    coll.optionsByOrderedIds[Analysis, Analysis.Id](ids, readPref = _.sec)(_.id)

  def associateToGames(games: List[Game]): Fu[List[(Game, Analysis)]] =
    byIds(games.map(g => Analysis.Id(g.id))).map: as =>
      games.zip(as).collect { case (game, Some(analysis)) =>
        game -> analysis
      }

  private[analyse] def save(analysis: Analysis) = coll.insert.one(analysis).void

  def remove(id: GameId) = coll.delete.one($id(Analysis.Id(id)))

  def remove(ids: List[GameId]) = coll.delete.one($inIds(ids.map(Analysis.Id(_))))

  def exists(id: GameId) = coll.exists($id(Analysis.Id(id)))
  def chapterExists(id: StudyChapterId) = coll.exists($id(id.value))
