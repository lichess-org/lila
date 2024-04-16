package lila.bot

import com.softwaremill.macwire.*

import lila.core.socket.IsOnline

@Module
final class Env(
    chatApi: lila.chat.ChatApi,
    gameRepo: lila.game.GameRepo,
    lightUserApi: lila.core.user.LightUserApi,
    rematches: lila.game.Rematches,
    isOfferingRematch: lila.core.round.IsOfferingRematch,
    spam: lila.core.security.SpamApi,
    isOnline: IsOnline
)(using Executor, akka.actor.ActorSystem, Scheduler, play.api.Mode, lila.core.i18n.Translator):

  lazy val jsonView = wire[BotJsonView]

  lazy val gameStateStream = wire[GameStateStream]

  lazy val player = wire[BotPlayer]

  lazy val onlineApiUsers: OnlineApiUsers = wire[OnlineApiUsers]

  val form = BotForm
