package lila.tv

import akka.actor._
import com.typesafe.config.Config

import lila.db.dsl._
import lila.game.Game

import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    lightUser: lila.common.LightUser.GetterSync,
    roundProxyGame: Game.ID => Fu[Option[Game]],
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    onSelect: Game => Unit
) {

  private val FeaturedSelect = config duration "featured.select"
  private val ChannelSelect = config getString "channel.select.name "

  private val selectChannel = system.actorOf(Props(classOf[lila.socket.Channel]), name = ChannelSelect)

  private val tvActor = system.actorOf(
    Props(new TvActor(hub.actor.renderer, hub.socket.round, selectChannel, lightUser, onSelect))
  )

  lazy val tv = new Tv(tvActor, roundProxyGame)

  {
    import scala.concurrent.duration._

    scheduler.message(FeaturedSelect) {
      tvActor -> TvActor.Select
    }
  }
}

object Env {

  lazy val current = "tv" boot new Env(
    config = lila.common.PlayApp loadConfig "tv",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    lightUser = lila.user.Env.current.lightUserSync,
    roundProxyGame = lila.round.Env.current.roundProxyGame _,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    onSelect = lila.round.Env.current.recentTvGames.put _
  )
}
