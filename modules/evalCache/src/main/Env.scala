package lila.evalCache

import com.typesafe.config.Config
import play.api.libs.json.JsValue
import scala.concurrent.duration._

import lila.hub.actorApi.socket.remote.{ TellSriIn, TellSriOut }
import lila.socket.Socket.Sri

final class Env(
    config: Config,
    settingStore: lila.memo.SettingStore.Builder,
    db: lila.db.Env,
    bus: lila.common.Bus,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val CollectionEvalCache = config getString "collection.eval_cache"

  private lazy val truster = new EvalCacheTruster(asyncCache)

  private lazy val upgrade = new EvalCacheUpgrade

  lazy val api = new EvalCacheApi(
    coll = db(CollectionEvalCache),
    truster = truster,
    upgrade = upgrade,
    asyncCache = asyncCache
  )

  private lazy val socketHandler = new EvalCacheSocketHandler(
    api = api,
    truster = truster,
    upgrade = upgrade
  )

  // remote socket support
  bus.subscribeFun(Symbol("remoteSocketIn:evalGet")) {
    case TellSriIn(sri, _, msg) => msg obj "d" foreach { d =>
      // TODO send once, let lila-ws distribute
      socketHandler.evalGet(Sri(sri), d, res => bus.publish(TellSriOut(sri, res), 'remoteSocketOut))
    }
  }
  bus.subscribeFun(Symbol("remoteSocketIn:evalPut")) {
    case TellSriIn(sri, Some(userId), msg) => msg obj "d" foreach { d =>
      socketHandler.untrustedEvalPut(Sri(sri), userId, d)
    }
  }
  // END remote socket support

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
    bus = lila.common.PlayApp.system.lilaBus,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
