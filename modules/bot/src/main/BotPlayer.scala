package lila.bot

import chess.format.Uci

import lila.common.Bus
import lila.core.round.*
import lila.core.shutup.PublicSource
import lila.game.GameExt.playerCanOfferDraw
import lila.game.{ GameRepo, Rematches }

final class BotPlayer(
    chatApi: lila.chat.ChatApi,
    gameRepo: GameRepo,
    rematches: Rematches,
    spam: lila.core.security.SpamApi
)(using Executor, Scheduler):

  private def clientError[A](msg: String): Fu[A] = fufail(lila.core.round.ClientError(msg))

  def apply(pov: Pov, uciStr: String, offeringDraw: Option[Boolean])(using me: Me): Funit =
    lila.common.LilaFuture.delay((pov.game.hasAi.so(500)).millis):
      Uci(uciStr).fold(clientError[Unit](s"Invalid UCI: $uciStr")): uci =>
        lila.mon.bot.moves(me.username.value).increment()
        if !pov.isMyTurn then clientError("Not your turn, or game already over")
        else
          val promise = Promise[Unit]()
          if pov.player.isOfferingDraw && offeringDraw.has(false) then declineDraw(pov)
          else if !pov.player.isOfferingDraw && ~offeringDraw then offerDraw(pov)
          tellRound(pov.gameId, RoundBus.BotPlay(pov.playerId, uci, promise.some))
          promise.future.recover:
            case _: lila.core.round.GameIsFinishedError if ~offeringDraw => ()

  def chat(gameId: GameId, d: BotForm.ChatData)(using me: Me) =
    (!spam.detect(d.text))
      .so(fuccess:
        lila.mon.bot.chats(me.username.value).increment()
        val chatId = ChatId(if d.room == "player" then gameId.value else s"$gameId/w")
        val source = (d.room == "spectator").option {
          PublicSource.Watcher(gameId)
        }
        chatApi.userChat.write(chatId, me, d.text, publicSource = source, _.round))

  def rematchAccept(id: GameId)(using Me): Fu[Boolean] = rematch(id, accept = true)

  def rematchDecline(id: GameId)(using Me): Fu[Boolean] = rematch(id, accept = false)

  private def rematch(challengeId: GameId, accept: Boolean)(using me: Me): Fu[Boolean] =
    rematches
      .prevGameIdOffering(challengeId)
      .so(gameRepo.game)
      .map:
        _.flatMap(Pov(_, me)).so { pov =>
          // delay so it feels more natural
          lila.common.LilaFuture.delay(if accept then 100.millis else 1.second):
            fuccess:
              tellRound(pov.gameId, RoundBus.Rematch(pov.playerId, accept))
          true
        }

  private def tellRound(id: GameId, msg: RoundBus) =
    Bus.pub(Tell(id, msg))

  def abort(pov: Pov): Funit =
    if !pov.game.abortableByUser then clientError("This game can no longer be aborted")
    else
      fuccess:
        tellRound(pov.gameId, RoundBus.Abort(pov.playerId))

  def resign(pov: Pov): Funit =
    if pov.game.abortableByUser then
      fuccess:
        tellRound(pov.gameId, RoundBus.Abort(pov.playerId))
    else if pov.game.resignable then
      fuccess:
        tellRound(pov.gameId, RoundBus.Resign(pov.playerId))
    else clientError("This game cannot be resigned")

  private def declineDraw(pov: Pov): Unit =
    if pov.game.drawable && pov.opponent.isOfferingDraw then
      tellRound(pov.gameId, RoundBus.Draw(pov.playerId, false))

  private def offerDraw(pov: Pov): Unit =
    if pov.game.drawable && (pov.game.playerCanOfferDraw(pov.color) || pov.opponent.isOfferingDraw) then
      tellRound(pov.gameId, RoundBus.Draw(pov.playerId, true))

  def setDraw(pov: Pov, v: Boolean): Unit =
    if v then offerDraw(pov) else declineDraw(pov)

  def setTakeback(pov: Pov, v: Boolean): Unit =
    if pov.game.playable && pov.game.canTakebackOrAddTime then
      tellRound(pov.gameId, RoundBus.Takeback(pov.playerId, v))

  def claimVictory(pov: Pov): Funit =
    pov.mightClaimWin.so:
      finishRoundThenFetchGame(pov, RoundBus.ResignForce(pov.playerId))
        .map:
          _.exists(_.winner.map(_.id).has(pov.playerId))
        .flatMap:
          if _ then funit
          else clientError("You cannot claim the win on this game")

  def claimDraw(pov: Pov): Funit =
    pov.game.drawable.so:
      finishRoundThenFetchGame(pov, RoundBus.DrawForce(pov.playerId))
        .map:
          _.exists(_.drawn)
        .flatMap:
          if _ then funit
          else clientError("You cannot claim draw on this game")

  private def finishRoundThenFetchGame(pov: Pov, event: RoundBus): Fu[Option[Game]] =
    tellRound(pov.gameId, event)
    lila.common.LilaFuture.delay(500.millis):
      gameRepo.finished(pov.gameId)

  def berserk(game: Game)(using me: Me): Boolean =
    game.berserkable.so:
      Bus.pub(Berserk(game.id, me))
      true
