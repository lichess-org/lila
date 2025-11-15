package lila.bot

import com.softwaremill.macwire.*

import lila.core.socket.IsOnline

@Module
final class Env(
    chatApi: lila.chat.ChatApi,
    gameRepo: lila.game.GameRepo,
    lightUserApi: lila.core.user.LightUserApi,
    rematches: lila.game.Rematches,
    spam: lila.core.security.SpamApi,
    isOnline: IsOnline,
    settingStore: lila.memo.SettingStore.Builder,
    cacheApi: lila.memo.CacheApi,
    userApi: lila.core.user.UserApi,
    userJsonView: lila.core.user.JsonView
)(using Executor, akka.actor.ActorSystem, Scheduler, lila.core.i18n.Translator, lila.core.config.RateLimit):

  lazy val limit = wire[BotLimit]

  lazy val jsonView = wire[BotJsonView]

  lazy val gameStateStream = wire[GameStateStream]

  lazy val player = wire[BotPlayer]

  lazy val onlineApiUsers: OnlineApiUsers = wire[OnlineApiUsers]

  lazy val boardReport = wire[BoardReport]

  val form = BotForm
