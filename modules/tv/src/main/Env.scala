package lila.tv

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import scala.concurrent.duration._

@Module
final class Env(
    gameRepo: lila.game.GameRepo,
    renderer: lila.hub.actors.Renderer,
    lightUser: lila.common.LightUser.GetterSync,
    gameProxyRepo: lila.round.GameProxyRepo,
    system: ActorSystem,
    recentTvGames: lila.round.RecentTvGames,
    rematches: lila.game.Rematches
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val tvSyncActor = wire[TvSyncActor]

  lazy val tv = wire[Tv]

  system.scheduler.scheduleWithFixedDelay(12 seconds, 3 seconds) { () =>
    tvSyncActor ! TvSyncActor.Select
  }
}
