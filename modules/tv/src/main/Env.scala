package lidraughts.tv

import akka.actor._
import com.typesafe.config.Config

import lidraughts.db.dsl._

import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    lightUser: lidraughts.common.LightUser.GetterSync,
    system: ActorSystem,
    scheduler: lidraughts.common.Scheduler
) {

  private val FeaturedSelect = config duration "featured.select"
  private val ChannelSelect = config getString "channel.select.name "

  private val selectChannel = system.actorOf(Props(classOf[lidraughts.socket.Channel]), name = ChannelSelect)

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
    config = lidraughts.common.PlayApp loadConfig "tv",
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    lightUser = lidraughts.user.Env.current.lightUserSync,
    system = lidraughts.common.PlayApp.system,
    scheduler = lidraughts.common.PlayApp.scheduler
  )
}
