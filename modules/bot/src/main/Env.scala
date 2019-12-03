package lila.bot

import com.softwaremill.macwire._

import lila.game.{ Game, Pov }

final class Env(
    chatApi: lila.chat.ChatApi,
    gameRepo: lila.game.GameRepo,
    lightUserApi: lila.user.LightUserApi,
    rematches: lila.game.Rematches,
    isOfferingRematch: lila.round.IsOfferingRematch
)(implicit system: akka.actor.ActorSystem) {

  private def scheduler = system.scheduler

  lazy val jsonView = wire[BotJsonView]

  lazy val gameStateStream = wire[GameStateStream]

  lazy val player = wire[BotPlayer]

  private lazy val onlineBots: OnlineBots = wire[OnlineBots]

  val setOnline = onlineBots.setOnline _

  val form = BotForm
}
