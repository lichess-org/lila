package lila.bot

import scala.concurrent.Promise

import chess.format.Uci

import lila.game.{ Game, Pov, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ BotPlay, RematchYes, RematchNo }
import lila.user.User

final class BotPlayer(roundMap: akka.actor.ActorSelection) {

  def apply(pov: Pov, uciStr: String): Funit =
    Uci(uciStr).fold(fufail[Unit](s"Invalid UCI: $uciStr")) { uci =>
      if (!pov.isMyTurn) fufail("Not your turn, or game already over")
      else {
        val promise = Promise[Unit]
        roundMap ! Tell(pov.gameId, BotPlay(pov.playerId, uci, promise.some))
        promise.future
      }
    }

  def rematchAccept(id: Game.ID, me: User): Fu[Boolean] =
    GameRepo game id map {
      _.flatMap(Pov(_, me)).filter(_.opponent.isOfferingRematch) ?? { pov =>
        roundMap ! Tell(pov.gameId, RematchYes(pov.playerId))
        true
      }
    }

  def rematchDecline(id: Game.ID, me: User): Fu[Boolean] =
    GameRepo game id map {
      _.flatMap(Pov(_, me)).filter(_.opponent.isOfferingRematch) ?? { pov =>
        roundMap ! Tell(pov.gameId, RematchNo(pov.playerId))
        true
      }
    }
}
