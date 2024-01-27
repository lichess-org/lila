package lila.analyse

import lila.common.Bus
import lila.game.actorApi.InsertGame
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.map.TellIfExists

final class Analyser(
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo
)(using Executor):

  def get(game: Game): Fu[Option[Analysis]] =
    analysisRepo byGame game

  def byId(id: Analysis.Id): Fu[Option[Analysis]] = analysisRepo byId id

  def save(analysis: Analysis): Funit =
    analysis.id match
      case Analysis.Id.Game(id) =>
        gameRepo.game(id).flatMapz { prev =>
          val game = prev.setAnalysed
          for
            _ <- gameRepo.setAnalysed(game.id)
            _ <- analysisRepo.save(analysis)
            _ <- sendAnalysisProgress(analysis, complete = true)
          yield
            Bus.publish(actorApi.AnalysisReady(game, analysis), "analysisReady")
            Bus.publish(InsertGame(game), "gameSearchInsert")
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
              actorApi.AnalysisProgress(
                analysis = analysis,
                game = g.game,
                variant = g.game.variant,
                initialFen = g.fen | g.game.variant.initialFen
              )
            ),
            "roundSocket"
          )
        }
      case _ =>
        fuccess:
          Bus.publish(actorApi.StudyAnalysisProgress(analysis, complete), "studyAnalysisProgress")
