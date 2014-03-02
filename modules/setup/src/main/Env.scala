package lila.setup

import akka.actor._
import com.typesafe.config.{ Config => AppConfig }

import lila.common.PimpedConfig._
import lila.game.{ Game, Progress }
import lila.user.UserContext

final class Env(
    config: AppConfig,
    db: lila.db.Env,
    hub: lila.hub.Env,
    aiPlay: Game => Fu[Progress],
    system: ActorSystem) {

  private val FriendMemoTtl = config duration "friend.memo.ttl"
  private val CollectionUserConfig = config getString "collection.user_config"
  private val CollectionAnonConfig = config getString "collection.anon_config"
  private val ChallengerName = config getString "challenger.name"

  lazy val forms = new FormFactory

  def filter(implicit ctx: UserContext): Fu[FilterConfig] =
    ctx.me.fold(AnonConfigRepo filter ctx.req)(UserConfigRepo.filter)

  lazy val processor = new Processor(
    lobby = hub.actor.lobby,
    friendConfigMemo = friendConfigMemo,
    router = hub.actor.router,
    aiPlay = aiPlay)

  lazy val friendJoiner = new FriendJoiner(
    friendConfigMemo = friendConfigMemo,
    router = hub.actor.router)

  lazy val friendConfigMemo = new FriendConfigMemo(ttl = FriendMemoTtl)

  system.actorOf(Props(new Challenger(
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
    aiPlay = lila.round.Env.current.aiPlay,
    system = lila.common.PlayApp.system)
}
