package lila.analyse

import monocle.syntax.all.*
import play.api.libs.json.*

import lila.common.Bus
import lila.tree.{ Analysis, Tree }

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
          yield Bus.pub(actorApi.AnalysisReady(game, analysis))
        }
      case _ =>
        analysisRepo.save(analysis) >>
          sendAnalysisProgress(analysis, complete = true)

  def progress(analysis: Analysis): Funit = sendAnalysisProgress(analysis, complete = false)

  private def sendAnalysisProgress(analysis: Analysis, complete: Boolean): Funit =
    analysis.id match
      case Analysis.Id.Game(id) =>
        gameRepo.gameWithInitialFen(id).mapz { g =>
          Bus.pub(
            lila.tree.AnalysisProgress(
              id,
              () => makeProgressPayload(analysis, g.game, g.fen | g.game.variant.initialFen)
            )
          )
        }
      case _ =>
        fuccess:
          Bus.pub(lila.tree.StudyAnalysisProgress(analysis, complete))

  private def makeProgressPayload(
      analysis: Analysis,
      game: Game,
      initialFen: chess.format.Fen.Full
  ): JsObject =
    Json.obj(
      "analysis" -> JsonView.bothPlayers(game.startedAtPly, analysis),
      "tree" -> Tree.makeJsonString(
        game,
        analysis.some,
        initialFen,
        logChessError = lila.log("analyser").warn
      )
    )
