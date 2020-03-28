package lidraughts.evalCache

import com.typesafe.config.Config

final class Env(
    config: Config,
    settingStore: lidraughts.memo.SettingStore.Builder,
    db: lidraughts.db.Env,
    system: akka.actor.ActorSystem,
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  private val CollectionEvalCache = config getString "collection.eval_cache"

  private lazy val truster = new EvalCacheTruster

  private lazy val upgrade = new EvalCacheUpgrade(asyncCache)

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

  system.lidraughtsBus.subscribeFun('socketLeave) {
    case lidraughts.socket.actorApi.SocketLeave(uid, _) => upgrade unregister uid
  }

  def cli = new lidraughts.common.Cli {
    def process = {
      case "eval-cache" :: "drop" :: fenParts =>
        api.drop(draughts.variant.Standard, draughts.format.FEN(fenParts mkString " ")) inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "evalCache" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "evalCache",
    settingStore = lidraughts.memo.Env.current.settingStore,
    db = lidraughts.db.Env.current,
    system = lidraughts.common.PlayApp.system,
    asyncCache = lidraughts.memo.Env.current.asyncCache
  )
}
