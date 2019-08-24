package lidraughts.bot

import akka.actor._

import lidraughts.game.Game

final class Env(
    system: ActorSystem,
    hub: lidraughts.hub.Env,
    onlineUserIds: lidraughts.memo.ExpireSetMemo,
    lightUserApi: lidraughts.user.LightUserApi,
    rematchOf: Game.ID => Option[Game.ID]
) {

  lazy val jsonView = new BotJsonView(lightUserApi, rematchOf)

  lazy val gameStateStream = new GameStateStream(system, jsonView)

  lazy val player = new BotPlayer(hub.chat)(system)

  val form = BotForm
}

object Env {

  lazy val current: Env = "bot" boot new Env(
    system = lidraughts.common.PlayApp.system,
    hub = lidraughts.hub.Env.current,
    onlineUserIds = lidraughts.user.Env.current.onlineUserIdMemo,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    rematchOf = lidraughts.game.Env.current.rematches.getIfPresent
  )
}
