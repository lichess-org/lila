package lila.setup

import lila.common.PimpedConfig._
import lila.user.Context

import com.typesafe.config.{ Config => AppConfig }
import akka.actor._

final class Env(
    config: AppConfig,
    db: lila.db.Env,
    system: ActorSystem) {

  private val FriendMemoTtl = config duration "friend.memo.ttl"
  private val CollectionUserConfig = config getString "collection.user_config"
  private val CollectionAnonConfig = config getString "collection.anon_config"

  lazy val forms = new FormFactory

  def filter(implicit ctx: Context): Fu[UserConfig] = forms savedConfig ctx

  private[setup] lazy val friendConfigMemo = new FriendConfigMemo(
    ttl = FriendMemoTtl)

  private[setup] lazy val userConfigColl = db(CollectionUserConfig)
  private[setup] lazy val anonConfigColl = db(CollectionAnonConfig)
}

object Env {

  lazy val current = "[boot] setup" describes new Env(
    config = lila.common.PlayApp loadConfig "setup",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system)
}
