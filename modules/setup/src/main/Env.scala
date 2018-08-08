package lidraughts.setup

import akka.actor._
import com.typesafe.config.{ Config => AppConfig }

import lidraughts.user.UserContext

final class Env(
    config: AppConfig,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    onStart: String => Unit,
    prefApi: lidraughts.pref.PrefApi,
    relationApi: lidraughts.relation.RelationApi,
    gameCache: lidraughts.game.Cached,
    system: ActorSystem
) {

  private val MaxPlaying = config getInt "max_playing"
  private val CollectionUserConfig = config getString "collection.user_config"
  private val CollectionAnonConfig = config getString "collection.anon_config"

  lazy val forms = new FormFactory

  def filter(ctx: UserContext): Fu[FilterConfig] =
    ctx.me.fold(AnonConfigRepo filter ctx.req)(UserConfigRepo.filter)

  lazy val processor = new Processor(
    lobby = hub.actor.lobby,
    gameCache = gameCache,
    maxPlaying = MaxPlaying,
    onStart = onStart
  )

  private[setup] lazy val userConfigColl = db(CollectionUserConfig)
  private[setup] lazy val anonConfigColl = db(CollectionAnonConfig)
}

object Env {

  lazy val current = "setup" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "setup",
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    onStart = lidraughts.game.Env.current.onStart,
    prefApi = lidraughts.pref.Env.current.api,
    relationApi = lidraughts.relation.Env.current.api,
    gameCache = lidraughts.game.Env.current.cached,
    system = lidraughts.common.PlayApp.system
  )
}
