package lila.bot

import chess.format.Uci

import lila.common.Bus
import lila.game.{ Game, GameRepo, Pov, Rematches }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ Abort, Berserk, BotPlay, RematchNo, RematchYes, Resign }
import lila.round.actorApi.round.{ DrawNo, DrawYes, ResignForce, TakebackNo, TakebackYes }
import lila.user.User

final class BotPlayer(
    chatApi: lila.chat.ChatApi,
    gameRepo: GameRepo,
    rematches: Rematches,
    spam: lila.security.Spam
)(using Executor, Scheduler):

  private def clientError[A](msg: String): Fu[A] = fufail(lila.round.ClientError(msg))

  def apply(pov: Pov, me: User, uciStr: String, offeringDraw: Option[Boolean]): Funit =
    lila.common.LilaFuture.delay((pov.game.hasAi ?? 500) millis) {
      Uci(uciStr).fold(clientError[Unit](s"Invalid UCI: $uciStr")) { uci =>
        lila.mon.bot.moves(me.username.value).increment()
        if (!pov.isMyTurn) clientError("Not your turn, or game already over")
        else
          val promise = Promise[Unit]()
          if (pov.player.isOfferingDraw && offeringDraw.has(false)) declineDraw(pov)
          else if (!pov.player.isOfferingDraw && ~offeringDraw) offerDraw(pov)
          tellRound(pov.gameId, BotPlay(pov.playerId, uci, promise.some))
          promise.future recover {
            case _: lila.round.GameIsFinishedError if ~offeringDraw => ()
          }
      }
    }

  def chat(gameId: GameId, me: User, d: BotForm.ChatData) =
    !spam.detect(d.text) ??
      fuccess {
        lila.mon.bot.chats(me.username.value).increment()
        val chatId = ChatId(if (d.room == "player") gameId.value else s"$gameId/w")
        val source = d.room == "spectator" option {
          lila.hub.actorApi.shutup.PublicSource.Watcher(gameId)
        }
        chatApi.userChat.write(chatId, me.id, d.text, publicSource = source, _.Round)
      }

  def rematchAccept(id: GameId, me: User): Fu[Boolean] = rematch(id, me, accept = true)

  def rematchDecline(id: GameId, me: User): Fu[Boolean] = rematch(id, me, accept = false)

  private def rematch(challengeId: GameId, me: User, accept: Boolean): Fu[Boolean] =
    rematches.prevGameIdOffering(challengeId) ?? gameRepo.game map {
      _.flatMap(Pov(_, me)) ?? { pov =>
        // delay so it feels more natural
        lila.common.LilaFuture.delay(if (accept) 100.millis else 1.second) {
          fuccess {
            tellRound(pov.gameId, if (accept) RematchYes(pov.playerId) else RematchNo(pov.playerId))
          }
        }
        true
      }
    }

  private def tellRound(id: GameId, msg: Any) =
    Bus.publish(Tell(id.value, msg), "roundSocket")

  def abort(pov: Pov): Funit =
    if (!pov.game.abortableByUser) clientError("This game can no longer be aborted")
    else
      fuccess {
        tellRound(pov.gameId, Abort(pov.playerId))
      }

  def resign(pov: Pov): Funit =
    if (pov.game.abortableByUser) fuccess {
      tellRound(pov.gameId, Abort(pov.playerId))
    }
    else if (pov.game.resignable) fuccess {
      tellRound(pov.gameId, Resign(pov.playerId))
    }
    else clientError("This game cannot be resigned")

  private def declineDraw(pov: Pov): Unit =
    if (pov.game.drawable && pov.opponent.isOfferingDraw)
      tellRound(pov.gameId, DrawNo(pov.playerId))

  private def offerDraw(pov: Pov): Unit =
    if (pov.game.drawable && (pov.game.playerCanOfferDraw(pov.color) || pov.opponent.isOfferingDraw))
      tellRound(pov.gameId, DrawYes(pov.playerId))

  def setDraw(pov: Pov, v: Boolean): Unit =
    if (v) offerDraw(pov) else declineDraw(pov)

  def setTakeback(pov: Pov, v: Boolean): Unit =
    if (pov.game.playable && pov.game.canTakebackOrAddTime)
      tellRound(pov.gameId, if v then TakebackYes(pov.playerId) else TakebackNo(pov.playerId))

  def claimVictory(pov: Pov): Funit =
    pov.mightClaimWin ?? {
      tellRound(pov.gameId, ResignForce(pov.playerId))
      lila.common.LilaFuture.delay(500 millis) {
        gameRepo.finished(pov.gameId) map {
          _.exists(_.winner.map(_.id) has pov.playerId)
        }
      }
    } flatMap {
      if _ then funit
      else clientError("You cannot claim the win on this game")
    }

  def berserk(game: Game, me: User): Boolean =
    game.berserkable ?? {
      Bus.publish(Berserk(game.id, me.id), "berserk")
      true
    }
