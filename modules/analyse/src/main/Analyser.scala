package lila.analyse

import monocle.syntax.all.*
import play.api.libs.json.*

import lila.common.Bus
import lila.core.misc.map.TellIfExists
import lila.tree.{ Analysis, ExportOptions, Tree }

final class Analyser(
    gameRepo: lila.core.game.GameRepo,
    analysisRepo: AnalysisRepo
)(using Executor)
    extends lila.tree.Analyser:

  def get(game: Game): Fu[Option[Analysis]] =
    analysisRepo.byGame(game)

  def byId(id: Analysis.Id): Fu[Option[Analysis]] = analysisRepo.byId(id)

  def save(analysis: Analysis): Funit =
    analysis.id match
      case Analysis.Id.Game(id) =>
        gameRepo.game(id).flatMapz { prev =>
          val game = prev.focus(_.metadata.analysed).replace(true)
          for
            _ <- gameRepo.setAnalysed(game.id, true)
            _ <- analysisRepo.save(analysis)
            _ <- sendAnalysisProgress(analysis, complete = true)
          yield Bus.publish(actorApi.AnalysisReady(game, analysis), "analysisReady")
        }
      case _ =>
        analysisRepo.save(analysis) >>
          sendAnalysisProgress(analysis, complete = true)

  def progress(analysis: Analysis): Funit = sendAnalysisProgress(analysis, complete = false)

  private def sendAnalysisProgress(analysis: Analysis, complete: Boolean): Funit =
    analysis.id match
      case Analysis.Id.Game(id) =>
        gameRepo.gameWithInitialFen(id).mapz { g =>
          Bus.publish(
            TellIfExists(
              id.value,
              lila.tree.AnalysisProgress: () =>
                makeProgressPayload(analysis, g.game, g.fen | g.game.variant.initialFen)
            ),
            "roundSocket"
          )
        }
      case _ =>
        fuccess:
          Bus.publish(lila.tree.StudyAnalysisProgress(analysis, complete), "studyAnalysisProgress")

  private def makeProgressPayload(
      analysis: Analysis,
      game: Game,
      initialFen: chess.format.Fen.Full
  ): JsObject =
    Json.obj(
      "analysis" -> JsonView.bothPlayers(game.startedAtPly, analysis),
      "tree" -> Tree.makeMinimalJsonString(
        game,
        analysis.some,
        initialFen,
        ExportOptions.default,
        logChessError = lila.log("analyser").warn
      )
    )
