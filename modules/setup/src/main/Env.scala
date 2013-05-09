package lila.setup

import lila.common.PimpedConfig._
import lila.user.Context

import com.typesafe.config.{ Config => AppConfig }
import akka.actor._

final class Env(
    config: AppConfig,
    db: lila.db.Env,
    hub: lila.hub.Env,
    fisherman: lila.lobby.Fisherman,
    messenger: lila.round.Messenger,
    ai: lila.ai.Ai,
    system: ActorSystem) {

  private val FriendMemoTtl = config duration "friend.memo.ttl"
  private val CollectionUserConfig = config getString "collection.user_config"
  private val CollectionAnonConfig = config getString "collection.anon_config"

  lazy val forms = new FormFactory

  def filter(implicit ctx: Context): Fu[FilterConfig] = 
    ctx.me.fold(AnonConfigRepo filter ctx.req)(UserConfigRepo.filter)

  lazy val processor = new Processor(
    friendConfigMemo = friendConfigMemo,
    fisherman = fisherman,
    timeline = hub.actor.timeline,
    router = hub.actor.router,
    ai = ai)

  lazy val rematcher = new Rematcher(
    messenger = messenger,
    router = hub.actor.router,
    timeline = hub.actor.timeline)

  lazy val friendJoiner = new FriendJoiner(
    messenger = messenger,
    router = hub.actor.router,
    timeline = hub.actor.timeline)

  lazy val hookJoiner = new HookJoiner(
    fisherman = fisherman,
    timeline = hub.actor.timeline,
    messenger = messenger)

  private[setup] lazy val friendConfigMemo = new FriendConfigMemo(
    ttl = FriendMemoTtl)

  private[setup] lazy val userConfigColl = db(CollectionUserConfig)
  private[setup] lazy val anonConfigColl = db(CollectionAnonConfig)
}

object Env {

  lazy val current = "[boot] setup" describes new Env(
    config = lila.common.PlayApp loadConfig "setup",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    fisherman = lila.lobby.Env.current.fisherman,
    messenger = lila.round.Env.current.messenger,
    ai = lila.ai.Env.current.ai,
    system = lila.common.PlayApp.system)
}
