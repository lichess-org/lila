package lila.tv

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.db.dsl._
import lila.game.Game

import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    lightUser: lila.common.LightUser.GetterSync,
    proxyGame: Game.ID => Fu[Option[Game]],
    system: ActorSystem,
    onSelect: Game => Unit,
    rematchOf: Game.ID => Option[Game.ID]
) {

  private val FeaturedSelect = config duration "featured.select"

  private val selectChannel = new lila.socket.Channel(system)
  system.lilaBus.subscribe(selectChannel, 'tvSelectChannel)

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
    config = lila.common.PlayApp loadConfig "tv",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    lightUser = lila.user.Env.current.lightUserSync,
    proxyGame = lila.round.Env.current.proxy.gameIfPresent _,
    system = lila.common.PlayApp.system,
    onSelect = lila.round.Env.current.recentTvGames.put _,
    rematchOf = lila.game.Env.current.rematches.getIfPresent
  )
}
