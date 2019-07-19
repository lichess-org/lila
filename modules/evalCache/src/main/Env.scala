package lila.evalCache

import com.typesafe.config.Config
import play.api.libs.json.JsValue
import scala.concurrent.duration._

import lila.hub.actorApi.socket.{ RemoteSocketTellSriIn, RemoteSocketTellSriOut }
import lila.socket.Socket.Sri

final class Env(
    config: Config,
    settingStore: lila.memo.SettingStore.Builder,
    db: lila.db.Env,
    system: akka.actor.ActorSystem,
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

  lazy val socketHandler = new EvalCacheSocketHandler(
    api = api,
    truster = truster,
    upgrade = upgrade
  )

  system.lilaBus.subscribeFun('socketLeave) {
    case lila.socket.actorApi.SocketLeave(sri, _) => upgrade unregister sri
  }

  // remote socket support
  system.lilaBus.subscribeFun(Symbol("remoteSocketIn:evalGet")) {
    case RemoteSocketTellSriIn(sri, _, msg) => msg obj "d" foreach { d =>
      socketHandler.evalGet(Sri(sri), d, res => system.lilaBus.publish(RemoteSocketTellSriOut(sri, res), 'remoteSocketOut))
    }
  }
  system.lilaBus.subscribeFun(Symbol("remoteSocketIn:evalPut")) {
    case RemoteSocketTellSriIn(sri, Some(userId), msg) => msg obj "d" foreach { d =>
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
    system = lila.common.PlayApp.system,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
