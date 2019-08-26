package lidraughts.bot

import akka.actor._

import scala.concurrent.duration._
import scala.concurrent.Promise

import draughts.format.Uci
import lidraughts.game.{ Game, Pov, GameRepo }
import lidraughts.hub.actorApi.map.Tell
import lidraughts.hub.actorApi.round.{ BotPlay, RematchYes, RematchNo, Abort, Resign }
import lidraughts.round.actorApi.round.{ DrawNo, DrawYes }
import lidraughts.user.User

final class BotPlayer(
    chatActor: ActorSelection,
    isOfferingRematch: Pov => Boolean
)(implicit system: ActorSystem) {

  def apply(pov: Pov, me: User, uciStr: String, offeringDraw: Option[Boolean]): Funit =
    lidraughts.common.Future.delay((pov.game.hasAi ?? 500) millis) {
      Uci(uciStr).fold(fufail[Unit](s"Invalid UCI: $uciStr")) { uci =>
        lidraughts.mon.bot.moves(me.username)()
        if (!pov.isMyTurn) fufail("Not your turn, or game already over")
        else {
          val promise = Promise[Unit]
          if (pov.player.isOfferingDraw && (offeringDraw contains false)) declineDraw(pov)
          else if (!pov.player.isOfferingDraw && (offeringDraw contains true)) offerDraw(pov)
          system.lidraughtsBus.publish(
            Tell(pov.gameId, BotPlay(pov.playerId, uci, promise.some)),
            'roundMapTell
          )
          promise.future
        }
      }
    }

  def chat(gameId: Game.ID, me: User, d: BotForm.ChatData) = fuccess {
    lidraughts.mon.bot.chats(me.username)()
    val chatId = lidraughts.chat.Chat.Id {
      if (d.room == "player") gameId else s"$gameId/w"
    }
    val source = d.room == "spectator" option {
      lidraughts.hub.actorApi.shutup.PublicSource.Watcher(gameId)
    }
    chatActor ! lidraughts.chat.actorApi.UserTalk(chatId, me.id, d.text, publicSource = source)
  }

  def rematchAccept(id: Game.ID, me: User): Fu[Boolean] = rematch(id, me, true)

  def rematchDecline(id: Game.ID, me: User): Fu[Boolean] = rematch(id, me, false)

  private def rematch(id: Game.ID, me: User, accept: Boolean): Fu[Boolean] =
    GameRepo game id map {
      _.flatMap(Pov(_, me)).filter(p => isOfferingRematch(!p)) ?? { pov =>
        // delay so it feels more natural
        lidraughts.common.Future.delay(if (accept) 100.millis else 2.seconds) {
          fuccess {
            system.lidraughtsBus.publish(
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
      system.lidraughtsBus.publish(
        Tell(pov.gameId, Abort(pov.playerId)),
        'roundMapTell
      )
    }

  def resign(pov: Pov): Funit =
    if (pov.game.abortable) abort(pov)
    else if (pov.game.resignable) fuccess {
      system.lidraughtsBus.publish(
        Tell(pov.gameId, Resign(pov.playerId)),
        'roundMapTell
      )
    }
    else fufail("This game cannot be resigned")

  def declineDraw(pov: Pov): Unit =
    if (pov.game.drawable && pov.opponent.isOfferingDraw)
      system.lidraughtsBus.publish(
        Tell(pov.gameId, DrawNo(pov.playerId)),
        'roundMapTell
      )

  def offerDraw(pov: Pov): Unit =
    if (pov.game.drawable && pov.game.playerCanOfferDraw(pov.color) && pov.isMyTurn)
      system.lidraughtsBus.publish(
        Tell(pov.gameId, DrawYes(pov.playerId)),
        'roundMapTell
      )
}
