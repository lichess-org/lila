package lila.bot

import com.softwaremill.macwire.*
import lila.socket.IsOnline

@Module
@annotation.nowarn("msg=unused")
final class Env(
    chatApi: lila.chat.ChatApi,
    gameRepo: lila.game.GameRepo,
    lightUserApi: lila.user.LightUserApi,
    rematches: lila.game.Rematches,
    isOfferingRematch: lila.round.IsOfferingRematch,
    spam: lila.security.Spam,
    isOnline: IsOnline
)(using Executor, akka.actor.ActorSystem, Scheduler, play.api.Mode):

  lazy val jsonView = wire[BotJsonView]

  lazy val gameStateStream = wire[GameStateStream]

  lazy val player = wire[BotPlayer]

  lazy val onlineApiUsers: OnlineApiUsers = wire[OnlineApiUsers]

  val form = BotForm
