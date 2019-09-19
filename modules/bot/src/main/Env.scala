package lila.bot

import akka.actor._

import lila.game.{ Game, Pov }

final class Env(
    system: ActorSystem,
    hub: lila.hub.Env,
    onlineUserIds: lila.memo.ExpireSetMemo,
    lightUserApi: lila.user.LightUserApi,
    rematchOf: Game.ID => Option[Game.ID],
    isOfferingRematch: Pov => Boolean
) {

  lazy val jsonView = new BotJsonView(lightUserApi, rematchOf)

  lazy val gameStateStream = new GameStateStream(system, jsonView)

  lazy val player = new BotPlayer(hub.chat, isOfferingRematch)(system)

  val form = BotForm
}

object Env {

  lazy val current: Env = "bot" boot new Env(
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    onlineUserIds = lila.user.Env.current.onlineUserIdMemo,
    lightUserApi = lila.user.Env.current.lightUserApi,
    rematchOf = lila.game.Env.current.rematches.getIfPresent,
    isOfferingRematch = lila.round.Env.current.isOfferingRematch
  )
}
