package lila.analyse

import lila.common.Bus
import lila.game.actorApi.InsertGame
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.map.TellIfExists

final class Analyser(
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import AnalyseBsonHandlers._

  def get(game: Game): Fu[Option[Analysis]] =
    analysisRepo byGame game

  def byId(id: Analysis.ID): Fu[Option[Analysis]] = analysisRepo byId id

  def save(analysis: Analysis): Funit =
    analysis.studyId match {
      case None =>
        gameRepo game analysis.id flatMap {
          _ ?? { prev =>
            val game = prev.setAnalysed
            gameRepo.setAnalysed(game.id) >>
              analysisRepo.save(analysis) >>
              sendAnalysisProgress(analysis, complete = true) >>- {
                Bus.publish(actorApi.AnalysisReady(game, analysis), "analysisReady")
                Bus.publish(InsertGame(game), "gameSearchInsert")
              }
          }
        }
      case Some(_) =>
        analysisRepo.save(analysis) >>
          sendAnalysisProgress(analysis, complete = true)
    }

  def progress(analysis: Analysis): Funit = sendAnalysisProgress(analysis, complete = false)

  private def sendAnalysisProgress(analysis: Analysis, complete: Boolean): Funit =
    analysis.studyId match {
      case None =>
        gameRepo gameWithInitialFen analysis.id map {
          _ ?? { case (game, initialFen) =>
            Bus.publish(
              TellIfExists(
                analysis.id,
                actorApi.AnalysisProgress(
                  analysis = analysis,
                  game = game,
                  variant = game.variant,
                  initialFen = initialFen | game.variant.initialFen
                )
              ),
              "roundSocket"
            )
          }
        }
      case Some(_) =>
        fuccess {
          Bus.publish(actorApi.StudyAnalysisProgress(analysis, complete), "studyAnalysisProgress")
        }
    }
}
