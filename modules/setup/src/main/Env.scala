package lila.setup

import akka.actor._
import com.typesafe.config.{ Config => AppConfig }

import lila.user.UserContext

final class Env(
    config: AppConfig,
    db: lila.db.Env,
    fishnetPlayer: lila.fishnet.Player,
    onStart: String => Unit,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi,
    gameCache: lila.game.Cached,
    system: ActorSystem
) {

  private val MaxPlaying = config getInt "max_playing"
  private val CollectionUserConfig = config getString "collection.user_config"
  private val CollectionAnonConfig = config getString "collection.anon_config"

  private lazy val anonConfigRepo = new AnonConfigRepo(db(CollectionAnonConfig))
  private lazy val userConfigRepo = new UserConfigRepo(db(CollectionUserConfig))

  lazy val forms = new FormFactory(anonConfigRepo, userConfigRepo)

  def filter(ctx: UserContext): Fu[FilterConfig] =
    ctx.me.fold(anonConfigRepo filter ctx.req)(userConfigRepo.filter)

  lazy val processor = new Processor(
    bus = system.lilaBus,
    gameCache = gameCache,
    maxPlaying = MaxPlaying,
    fishnetPlayer = fishnetPlayer,
    anonConfigRepo = anonConfigRepo,
    userConfigRepo = userConfigRepo,
    onStart = onStart
  )
}

object Env {

  lazy val current = "setup" boot new Env(
    config = lila.common.PlayApp loadConfig "setup",
    db = lila.db.Env.current,
    fishnetPlayer = lila.fishnet.Env.current.player,
    onStart = lila.game.Env.current.onStart,
    prefApi = lila.pref.Env.current.api,
    relationApi = lila.relation.Env.current.api,
    gameCache = lila.game.Env.current.cached,
    system = lila.common.PlayApp.system
  )
}
