package lila.analyse

import akka.actor.ActorSelection

import chess.format.FEN
import lila.db.dsl._
import lila.game.actorApi.InsertGame
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.map.Tell

final class Analyser(
    indexer: ActorSelection,
    requesterApi: RequesterApi,
    roundSocket: ActorSelection,
    bus: lila.common.Bus) {

  def get(id: String): Fu[Option[Analysis]] = AnalysisRepo byId id

  def save(analysis: Analysis): Funit = GameRepo game analysis.id flatMap {
    _ ?? { game =>
      GameRepo.setAnalysed(game.id)
      AnalysisRepo.save(analysis) >>
        sendAnalysisProgress(analysis) >>- {
          bus.publish(actorApi.AnalysisReady(game, analysis), 'analysisReady)
          indexer ! InsertGame(game)
          requesterApi save analysis
        }
    }
  }

  def progress(analysis: Analysis): Funit = sendAnalysisProgress(analysis)

  private def sendAnalysisProgress(analysis: Analysis): Funit =
    GameRepo gameWithInitialFen analysis.id map {
      _ ?? {
        case (game, initialFen) =>
          roundSocket ! Tell(analysis.id, actorApi.AnalysisProgress(
            analysis = analysis,
            game = game,
            variant = game.variant,
            initialFen = initialFen | FEN(game.variant.initialFen)))
      }
    }
}
