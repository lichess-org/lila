package lila.bot

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Promise

import chess.format.Uci

import lila.game.{ Game, Pov, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ BotPlay, RematchYes, RematchNo, Abort }
import lila.user.User

final class BotPlayer(
    roundMap: ActorSelection,
    chatActor: ActorSelection,
    system: ActorSystem
) {

  def apply(pov: Pov, uciStr: String): Funit =
    Uci(uciStr).fold(fufail[Unit](s"Invalid UCI: $uciStr")) { uci =>
      if (!pov.isMyTurn) fufail("Not your turn, or game already over")
      else {
        val promise = Promise[Unit]
        roundMap ! Tell(pov.gameId, BotPlay(pov.playerId, uci, promise.some))
        promise.future
      }
    }

  def chat(gameId: Game.ID, me: User, d: BotForm.ChatData) = fuccess {
    val chatId = lila.chat.Chat.Id {
      if (d.room == "player") gameId else s"$gameId/w"
    }
    val source = d.room == "spectator" option {
      lila.hub.actorApi.shutup.PublicSource.Watcher(gameId)
    }
    chatActor ! lila.chat.actorApi.UserTalk(chatId, me.id, d.text, publicSource = source)
  }

  def rematchAccept(id: Game.ID, me: User): Fu[Boolean] = rematch(id, me, true)

  def rematchDecline(id: Game.ID, me: User): Fu[Boolean] = rematch(id, me, false)

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

  def abort(pov: Pov): Funit =
    if (!pov.game.abortable) fufail("This game can no longer be aborted")
    else fuccess {
      roundMap ! Tell(pov.gameId, Abort(pov.playerId))
    }
}
