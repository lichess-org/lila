package lila.tv

import akka.actor.{ActorSystem, Props}
import com.softwaremill.macwire.*

@Module
@annotation.nowarn("msg=unused")
final class Env(
    gameRepo: lila.game.GameRepo,
    renderer: lila.hub.actors.Renderer,
    lightUserApi: lila.user.LightUserApi,
    lightUserSync: lila.common.LightUser.GetterSync,
    gameProxyRepo: lila.round.GameProxyRepo,
    system: ActorSystem,
    recentTvGames: lila.round.RecentTvGames,
    rematches: lila.game.Rematches
)(using Executor):

  private val tvSyncActor = wire[TvSyncActor]

  lazy val tv = wire[Tv]

  val channelBroadcasts = Tv.Channel.values.map { c =>
    c -> system.actorOf(Props(wire[TvBroadcast]))
  }.toMap

  system.scheduler.scheduleWithFixedDelay(12 seconds, 3 seconds) { () =>
    tvSyncActor ! TvSyncActor.Select
  }
