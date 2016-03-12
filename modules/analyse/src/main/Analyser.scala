package lila.analyse

import akka.actor.ActorSelection

import lila.game.actorApi.InsertGame
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.AnalysisAvailable

final class Analyser(
    indexer: ActorSelection,
    roundMap: ActorSelection,
    bus: lila.common.Bus) {

  def get(id: String): Fu[Option[Analysis]] = AnalysisRepo byId id

  def save(analysis: Analysis): Funit = GameRepo game analysis.id flatMap {
    _ ?? { game =>
      GameRepo.setAnalysed(game.id)
      AnalysisRepo.save(analysis) >>- {
        bus.publish(actorApi.AnalysisReady(game, analysis), 'analysisReady)
        roundMap ! Tell(game.id, AnalysisAvailable)
        indexer ! InsertGame(game)
      }
    }
  }
}
