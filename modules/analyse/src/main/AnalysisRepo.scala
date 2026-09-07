package lila.analyse

import lila.db.dsl.{ *, given }
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

  def byHash(workHash: Array[Byte]): Fu[Option[Analysis]] =
    coll.one[Analysis]($doc("hash" -> workHash))

  private[analyse] def save(analysis: Analysis, workHash: Option[Array[Byte]]) =
    val bson = toBdoc(analysis).get ++ workHash.so(h => $doc("hash" -> h))
    coll.insert.one(bson).void

  def remove(id: GameId) = coll.delete.one($id(Analysis.Id(id)))

  def removeChapters(ids: Seq[StudyChapterId]) = coll.delete.one($inIds(ids.map(_.value)))
  def setOrphans(id: Seq[StudyChapterId]) = coll.updateField($inIds(id.map(_.value)), "orphan", true)

  def remove(ids: List[GameId]) = coll.delete.one($inIds(ids.map(Analysis.Id(_))))

  def exists(id: GameId) = coll.exists($id(Analysis.Id(id)))
  def chapterExists(id: StudyChapterId) = coll.exists($id(id.value))
