package lila.gameSearch

import lila.game.PgnRepo
import lila.game.actorApi._
import lila.search.{ actorApi ⇒ S }

import akka.actor._

private[gameSearch] final class Indexer(
    lowLevel: ActorRef,
    isAnalyzed: String ⇒ Fu[Boolean]) extends Actor {

  def receive = {

    case InsertGame(game) ⇒ PgnRepo getOption game.id foreach {
      _ foreach { pgn ⇒
        isAnalyzed(game.id) foreach { analyzed ⇒
          lowLevel ! S.InsertOne(game.id, Game.from(game, pgn, analyzed))
        }
      }
    }
  }
}
