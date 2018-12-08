package lila.evalCache

import com.typesafe.config.Config

final class Env(
    config: Config,
    settingStore: lila.memo.SettingStore.Builder,
    db: lila.db.Env,
    system: akka.actor.ActorSystem,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val CollectionEvalCache = config getString "collection.eval_cache"

  private lazy val truster = new EvalCacheTruster

  private lazy val upgrade = new EvalCacheUpgrade(
    asyncCache = asyncCache,
    enabled = upgradeEnabledSetting.get
  )

  val upgradeEnabledSetting = settingStore[Boolean](
    "cloudUpgradeEnabled",
    default = true,
    text = "Enable cloud eval upgrade for everyone.".some
  )

  lazy val api = new EvalCacheApi(
    coll = db(CollectionEvalCache),
    truster = truster,
    upgrade = upgrade,
    asyncCache = asyncCache
  )

  lazy val socketHandler = new EvalCacheSocketHandler(
    api = api,
    truster = truster,
    upgrade = upgrade
  )

  system.lilaBus.subscribeFun('socketLeave) {
    case lila.socket.actorApi.SocketLeave(uid, _) => upgrade unregister uid
  }

  def cli = new lila.common.Cli {
    def process = {
      case "eval-cache" :: "drop" :: fenParts =>
        api.drop(chess.variant.Standard, chess.format.FEN(fenParts mkString " ")) inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "evalCache" boot new Env(
    config = lila.common.PlayApp loadConfig "evalCache",
    settingStore = lila.memo.Env.current.settingStore,
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
