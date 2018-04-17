package lila.bot

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Promise

import chess.format.Uci

import lila.game.{ Game, Pov, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ BotPlay, RematchYes, RematchNo }
import lila.user.User

final class BotPlayer(roundMap: ActorSelection, system: ActorSystem) {

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
    rematch(id, me, true)

  def rematchDecline(id: Game.ID, me: User): Fu[Boolean] =
    rematch(id, me, false)

  private def rematch(id: Game.ID, me: User, accept: Boolean): Fu[Boolean] =
    GameRepo game id map {
      _.flatMap(Pov(_, me)).filter(_.opponent.isOfferingRematch) ?? { pov =>
        // delay so it feels more natural
        lila.common.Future.delay(accept.fold(100, 2000) millis) {
          fuccess {
            roundMap ! Tell(pov.gameId, accept.fold(RematchYes, RematchNo)(pov.playerId))
          }
        }(system)
        true
      }
    }
}
