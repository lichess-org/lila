package lidraughts.tv

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lidraughts.db.dsl._
import lidraughts.game.Game

import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    lightUser: lidraughts.common.LightUser.GetterSync,
    proxyGame: Game.ID => Fu[Option[Game]],
    system: ActorSystem,
    onSelect: Game => Unit,
    rematchOf: Game.ID => Option[Game.ID]
) {

  private val FeaturedSelect = config duration "featured.select"

  private val selectChannel = new lidraughts.socket.Channel(system)
  system.lidraughtsBus.subscribe(selectChannel, 'tvSelectChannel)

  private val tvTrouper = new TvTrouper(
    system,
    hub.renderer,
    selectChannel,
    lightUser,
    onSelect,
    proxyGame,
    rematchOf
  )

  lazy val tv = new Tv(tvTrouper, proxyGame)

  system.scheduler.schedule(10 seconds, FeaturedSelect) {
    tvTrouper ! TvTrouper.Select
  }
}

object Env {

  lazy val current = "tv" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "tv",
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    lightUser = lidraughts.user.Env.current.lightUserSync,
    proxyGame = lidraughts.round.Env.current.proxy.gameIfPresent _,
    system = lidraughts.common.PlayApp.system,
    onSelect = lidraughts.round.Env.current.recentTvGames.put _,
    rematchOf = lidraughts.game.Env.current.rematches.getIfPresent
  )
}
