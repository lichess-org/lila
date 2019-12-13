package lila.setup

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._
import lila.user.UserContext

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    fishnetPlayer: lila.fishnet.Player,
    onStart: lila.round.OnStart,
    gameCache: lila.game.Cached
) {

  private lazy val maxPlaying     = appConfig.get[Max]("setup.max_playing")
  private lazy val anonConfigRepo = new AnonConfigRepo(db(CollName("config")))
  private lazy val userConfigRepo = new UserConfigRepo(db(CollName("config_anon")))

  lazy val forms = wire[FormFactory]

  val filter: UserContext => Fu[FilterConfig] = ctx =>
    ctx.me.fold(anonConfigRepo filter ctx.req)(userConfigRepo.filter)

  lazy val processor = wire[Processor]
}
