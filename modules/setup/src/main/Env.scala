package lidraughts.setup

import akka.actor._
import com.typesafe.config.{ Config => AppConfig }

import lidraughts.user.UserContext

final class Env(
    config: AppConfig,
    db: lidraughts.db.Env,
    draughtsnetPlayer: lidraughts.draughtsnet.Player,
    onStart: String => Unit,
    prefApi: lidraughts.pref.PrefApi,
    relationApi: lidraughts.relation.RelationApi,
    gameCache: lidraughts.game.Cached,
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
    bus = system.lidraughtsBus,
    gameCache = gameCache,
    maxPlaying = MaxPlaying,
    draughtsnetPlayer = draughtsnetPlayer,
    anonConfigRepo = anonConfigRepo,
    userConfigRepo = userConfigRepo,
    onStart = onStart
  )
}

object Env {

  lazy val current = "setup" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "setup",
    db = lidraughts.db.Env.current,
    draughtsnetPlayer = lidraughts.draughtsnet.Env.current.player,
    onStart = lidraughts.round.Env.current.onStart,
    prefApi = lidraughts.pref.Env.current.api,
    relationApi = lidraughts.relation.Env.current.api,
    gameCache = lidraughts.game.Env.current.cached,
    system = lidraughts.common.PlayApp.system
  )
}
