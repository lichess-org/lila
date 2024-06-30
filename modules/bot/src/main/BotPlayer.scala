package lila.bot

import scala.concurrent.duration._
import scala.concurrent.Promise

import shogi.format.usi.{ UciToUsi, Usi }
import lila.common.Bus
import lila.game.FairyConversion.Kyoto
import lila.game.Game.PlayerId
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ Abort, BotPlay, RematchNo, RematchYes, Resign }
import lila.round.actorApi.round.{ DrawNo, DrawYes }
import lila.user.User

final class BotPlayer(
    chatApi: lila.chat.ChatApi,
    gameRepo: GameRepo,
    isOfferingRematch: lila.round.IsOfferingRematch
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  private def clientError[A](msg: String): Fu[A] = fufail(lila.round.ClientError(msg))

  def apply(pov: Pov, me: User, usiStr: String, offeringDraw: Option[Boolean]): Funit =
    lila.common.Future.delay((!pov.game.hasHuman ?? 750) millis) {
      Usi(usiStr)
        .orElse(UciToUsi(usiStr))
        .orElse(Kyoto.readFairyUsi(usiStr))
        .fold(clientError[Unit](s"Invalid USI: $usiStr")) { usi =>
          lila.mon.bot.moves(me.username).increment()
          if (!pov.isMyTurn) clientError("Not your turn, or game already over")
          else {
            val promise = Promise[Unit]()
            if (pov.player.isOfferingDraw && (offeringDraw contains false)) declineDraw(pov)
            else if (!pov.player.isOfferingDraw && ~offeringDraw) offerDraw(pov)
            tellRound(pov.gameId, BotPlay(pov.playerId, usi, promise.some))
            promise.future
          }
        }
    }

  def chat(gameId: Game.ID, me: User, d: BotForm.ChatData) =
    fuccess {
      lila.mon.bot.chats(me.username).increment()
      val chatId = lila.chat.Chat.Id {
        if (d.room == "player") gameId else s"$gameId/w"
      }
      val source = d.room == "spectator" option {
        lila.hub.actorApi.shutup.PublicSource.Watcher(gameId)
      }
      chatApi.userChat.write(chatId, me.id, d.text, publicSource = source, _.Round)
    }

  def rematchAccept(id: Game.ID, me: User): Fu[Boolean] = rematch(id, me, true)

  def rematchDecline(id: Game.ID, me: User): Fu[Boolean] = rematch(id, me, false)

  private def rematch(id: Game.ID, me: User, accept: Boolean): Fu[Boolean] =
    gameRepo game id map {
      _.flatMap(Pov(_, me)).filter(p => isOfferingRematch(p.gameId, !p.color)) ?? { pov =>
        // delay so it feels more natural
        lila.common.Future.delay(if (accept) 100.millis else 2.seconds) {
          fuccess {
            tellRound(pov.gameId, (if (accept) RematchYes else RematchNo) (pov.playerId))
          }
        }
        true
      }
    }

  private def tellRound(id: Game.ID, msg: Any) =
    Bus.publish(Tell(id, msg), "roundSocket")

  def abort(pov: Pov): Funit =
    if (!pov.game.abortable) clientError("This game can no longer be aborted")
    else
      fuccess {
        tellRound(pov.gameId, Abort(pov.playerId))
      }

  def resign(pov: Pov): Funit =
    if (pov.game.abortable) abort(pov)
    else if (pov.game.resignable) fuccess {
      tellRound(pov.gameId, Resign(pov.playerId))
    }
    else clientError("This game cannot be resigned")

  def declineDraw(pov: Pov): Unit =
    if (pov.game.drawable && pov.opponent.isOfferingDraw)
      tellRound(pov.gameId, DrawNo(PlayerId(pov.playerId)))

  def offerDraw(pov: Pov): Unit =
    if (pov.game.drawable && pov.game.playerCanOfferDraw(pov.color))
      tellRound(pov.gameId, DrawYes(PlayerId(pov.playerId)))

  def setDraw(pov: Pov, v: Boolean): Unit =
    if (v) offerDraw(pov) else declineDraw(pov)
}
