package lila.tv

import akka.actor._
import com.typesafe.config.Config

import lila.db.dsl._

import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    lightUser: lila.common.LightUser.GetterSync,
    system: ActorSystem,
    scheduler: lila.common.Scheduler
) {

  private val FeaturedSelect = config duration "featured.select"
  private val ChannelSelect = config getString "channel.select.name "

  private val selectChannel = system.actorOf(Props(classOf[lila.socket.Channel]), name = ChannelSelect)

  lazy val tv = new Tv(tvActor)

  private val tvActor =
    system.actorOf(
      Props(new TvActor(hub.actor.renderer, hub.socket.round, selectChannel, lightUser)),
      name = "tv"
    )

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
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler
  )
}
