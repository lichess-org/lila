package lila.setup

import akka.actor._
import com.typesafe.config.{ Config ⇒ AppConfig }

import lila.common.PimpedConfig._
import lila.user.Context

final class Env(
    config: AppConfig,
    db: lila.db.Env,
    hub: lila.hub.Env,
    messenger: lila.round.Messenger,
    ai: lila.ai.Ai,
    system: ActorSystem) {

  private val FriendMemoTtl = config duration "friend.memo.ttl"
  private val CollectionUserConfig = config getString "collection.user_config"
  private val CollectionAnonConfig = config getString "collection.anon_config"
  private val ChallengerName = config getString "challenger.name"

  lazy val forms = new FormFactory

  def filter(implicit ctx: Context): Fu[FilterConfig] =
    ctx.me.fold(AnonConfigRepo filter ctx.req)(UserConfigRepo.filter)

  lazy val processor = new Processor(
    lobby = hub.actor.lobby,
    friendConfigMemo = friendConfigMemo,
    timeline = hub.actor.gameTimeline,
    router = hub.actor.router,
    engine = ai)

  lazy val friendJoiner = new FriendJoiner(
    messenger = messenger,
    router = hub.actor.router,
    timeline = hub.actor.gameTimeline)

  lazy val friendConfigMemo = new FriendConfigMemo(ttl = FriendMemoTtl)

  system.actorOf(Props(new Challenger(
    bus = system.eventStream,
    roundHub = hub.socket.round,
    renderer = hub.actor.renderer
  )), name = ChallengerName)

  private[setup] lazy val userConfigColl = db(CollectionUserConfig)
  private[setup] lazy val anonConfigColl = db(CollectionAnonConfig)
}

object Env {

  lazy val current = "[boot] setup" describes new Env(
    config = lila.common.PlayApp loadConfig "setup",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    messenger = lila.round.Env.current.messenger,
    ai = lila.ai.Env.current.ai,
    system = lila.common.PlayApp.system)
}
