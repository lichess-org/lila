package lila.gameSearch

import akka.actor._

import lila.game.actorApi._
import lila.search.{ actorApi ⇒ S }

private[gameSearch] final class Indexer(
    lowLevel: ActorRef,
    isAnalyzed: String ⇒ Fu[Boolean]) extends Actor {

  def receive = {

    case InsertGame(game) ⇒ if (game.finished) {
      isAnalyzed(game.id) foreach { analyzed ⇒
        lowLevel ! S.InsertOne(game.id, Game.from(game, analyzed))
      }
    }
  }
}
