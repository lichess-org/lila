package lila.bot

import akka.actor._

import scala.concurrent.duration._
import scala.concurrent.Promise

import chess.format.Uci
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ Abort, BotPlay, RematchNo, RematchYes, Resign }
import lila.round.actorApi.round.{ DrawNo, DrawYes }
import lila.user.User

final class BotPlayer(
    chatActor: ActorSelection
)(implicit system: ActorSystem) {

  def apply(pov: Pov, me: User, uciStr: String, offeringDraw: Option[Boolean]): Funit =
    lila.common.Future.delay((pov.game.hasAi ?? 500) millis) {
      Uci(uciStr).fold(fufail[Unit](s"Invalid UCI: $uciStr")) { uci =>
        lila.mon.bot.moves(me.username)()
        if (!pov.isMyTurn) fufail("Not your turn, or game already over")
        else {
          val promise = Promise[Unit]
          if (pov.player.isOfferingDraw && (offeringDraw contains false)) declineDraw(pov)
          else if (!pov.player.isOfferingDraw && (offeringDraw contains true)) offerDraw(pov)
          system.lilaBus.publish(
            Tell(pov.gameId, BotPlay(pov.playerId, uci, promise.some)),
            'roundMapTell
          )
          promise.future
        }
      }
    }

  def chat(gameId: Game.ID, me: User, d: BotForm.ChatData) = fuccess {
    lila.mon.bot.chats(me.username)()
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
        lila.common.Future.delay(if (accept) 100.millis else 2.seconds) {
          fuccess {
            system.lilaBus.publish(
              Tell(pov.gameId, (if (accept) RematchYes else RematchNo)(pov.playerId)),
              'roundMapTell
            )
          }
        }(system)
        true
      }
    }

  def abort(pov: Pov): Funit =
    if (!pov.game.abortable) fufail("This game can no longer be aborted")
    else fuccess {
      system.lilaBus.publish(
        Tell(pov.gameId, Abort(pov.playerId)),
        'roundMapTell
      )
    }

  def resign(pov: Pov): Funit =
    if (pov.game.abortable) abort(pov)
    else if (pov.game.resignable) fuccess {
      system.lilaBus.publish(
        Tell(pov.gameId, Resign(pov.playerId)),
        'roundMapTell
      )
    }
    else fufail("This game cannot be resigned")

  def declineDraw(pov: Pov): Unit =
    if (pov.game.drawable && pov.opponent.isOfferingDraw)
      system.lilaBus.publish(
        Tell(pov.gameId, DrawNo(pov.playerId)),
        'roundMapTell
      )

  def offerDraw(pov: Pov): Unit =
    if (pov.game.drawable && pov.game.playerCanOfferDraw(pov.color) && pov.isMyTurn)
      system.lilaBus.publish(
        Tell(pov.gameId, DrawYes(pov.playerId)),
        'roundMapTell
      )
}
