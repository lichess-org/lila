package lila.setup

import akka.actor._
import com.typesafe.config.{ Config => AppConfig }

import lila.common.PimpedConfig._
import lila.game.{ Game, Pov, Progress }
import lila.user.UserContext

final class Env(
    config: AppConfig,
    db: lila.db.Env,
    hub: lila.hub.Env,
    fishnetPlayer: lila.fishnet.Player,
    onStart: String => Unit,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi,
    gameCache: lila.game.Cached,
    system: ActorSystem) {

  private val FriendMemoTtl = config duration "friend.memo.ttl"
  private val MaxPlaying = config getInt "max_playing"
  private val CollectionUserConfig = config getString "collection.user_config"
  private val CollectionAnonConfig = config getString "collection.anon_config"

  val CasualOnly = config getBoolean "casual_only"

  lazy val forms = new FormFactory(CasualOnly)

  def filter(ctx: UserContext): Fu[FilterConfig] =
    ctx.me.fold(AnonConfigRepo filter ctx.req)(UserConfigRepo.filter)

  lazy val processor = new Processor(
    lobby = hub.actor.lobby,
    gameCache = gameCache,
    maxPlaying = MaxPlaying,
    fishnetPlayer = fishnetPlayer,
    onStart = onStart)

  private[setup] lazy val userConfigColl = db(CollectionUserConfig)
  private[setup] lazy val anonConfigColl = db(CollectionAnonConfig)
}

object Env {

  lazy val current = "setup" boot new Env(
    config = lila.common.PlayApp loadConfig "setup",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    fishnetPlayer = lila.fishnet.Env.current.player,
    onStart = lila.game.Env.current.onStart,
    prefApi = lila.pref.Env.current.api,
    relationApi = lila.relation.Env.current.api,
    gameCache = lila.game.Env.current.cached,
    system = lila.common.PlayApp.system)
}
