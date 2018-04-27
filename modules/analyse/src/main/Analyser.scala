package lila.analyse

import akka.actor.ActorSelection

import chess.format.FEN
import lila.game.actorApi.InsertGame
import lila.game.{ GameRepo, Game }
import lila.hub.actorApi.map.Tell

final class Analyser(
    indexer: ActorSelection,
    requesterApi: RequesterApi,
    roundSocket: ActorSelection,
    studyActor: ActorSelection,
    bus: lila.common.Bus
) {

  def get(game: Game): Fu[Option[Analysis]] =
    AnalysisRepo byGame game

  def byId(id: Analysis.ID): Fu[Option[Analysis]] = AnalysisRepo byId id

  def save(analysis: Analysis): Funit = analysis.studyId match {
    case None => GameRepo game analysis.id flatMap {
      _ ?? { game =>
        GameRepo.setAnalysed(game.id)
        AnalysisRepo.save(analysis) >>
          sendAnalysisProgress(analysis, complete = true) >>- {
            bus.publish(actorApi.AnalysisReady(game, analysis), 'analysisReady)
            indexer ! InsertGame(game)
            requesterApi save analysis
          }
      }
    }
    case Some(studyId) =>
      AnalysisRepo.save(analysis) >>
        sendAnalysisProgress(analysis, complete = true) >>- {
          requesterApi save analysis
        }
  }

  def progress(analysis: Analysis): Funit = sendAnalysisProgress(analysis, complete = false)

  private def sendAnalysisProgress(analysis: Analysis, complete: Boolean): Funit = analysis.studyId match {
    case None => GameRepo gameWithInitialFen analysis.id map {
      _ ?? {
        case (game, initialFen) =>
          roundSocket ! Tell(analysis.id, actorApi.AnalysisProgress(
            analysis = analysis,
            game = game,
            variant = game.variant,
            initialFen = initialFen | FEN(game.variant.initialFen)
          ))
      }
    }
    case Some(studyId) => fuccess {
      studyActor ! actorApi.StudyAnalysisProgress(analysis, complete)
    }
  }
}
