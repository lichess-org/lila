package lila.gameSearch

import akka.actor._

import lila.game.actorApi.{ InsertGame, FinishGame }
import lila.search.{ actorApi => S }

private[gameSearch] final class Indexer(
    lowLevel: ActorRef,
    isAnalyzed: String => Fu[Boolean]) extends Actor {

  // context.system.lilaBus.subscribe(self, 'finishGame)

  def receive = {

    case FinishGame(game, _, _) => self ! InsertGame(game)

    case InsertGame(game) => if (game.finished) {
      isAnalyzed(game.id) foreach { analyzed =>
        lowLevel ! S.InsertOne(game.id, Game.from(game, analyzed))
      }
    }
  }
}
