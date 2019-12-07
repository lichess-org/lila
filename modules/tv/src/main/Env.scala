package lila.tv

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import play.api.Configuration
import scala.concurrent.duration._

import lila.db.dsl._
import lila.game.Game

import scala.concurrent.duration._

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    renderer: lila.hub.actors.Renderer,
    lightUser: lila.common.LightUser.GetterSync,
    gameProxyRepo: lila.round.GameProxyRepo,
    system: ActorSystem,
    recentTvGames: lila.round.RecentTvGames,
    rematches: lila.game.Rematches
) {

  private val tvTrouper = wire[TvTrouper]

  lazy val tv = wire[Tv]

  system.scheduler.scheduleWithFixedDelay(12 seconds, 3 seconds) { () =>
    tvTrouper ! TvTrouper.Select
  }
}
