package lila.evalCache

import chess.variant.Variant
import com.softwaremill.macwire._
import com.softwaremill.tagging._
import play.api.Configuration

import lila.common.Bus
import lila.common.config.CollName
import lila.hub.actorApi.socket.remote.{ TellSriIn, TellSriOut }
import lila.socket.Socket.Sri

@Module
final class Env(
    appConfig: Configuration,
    userRepo: lila.user.UserRepo,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    cacheApi: lila.memo.CacheApi,
    settingStore: lila.memo.SettingStore.Builder
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler,
    mode: play.api.Mode
) {

  private lazy val coll = yoloDb(appConfig.get[CollName]("evalCache.collection.evalCache")).failingSilently()

  private lazy val truster = wire[EvalCacheTruster]

  lazy val enable = settingStore[Boolean](
    "useCeval",
    default = true,
    text = "Enable cloud eval (disable in case of server trouble)".some
  )

  private lazy val upgrade = wire[EvalCacheUpgrade]

  lazy val api: EvalCacheApi = wire[EvalCacheApi]

  private lazy val socketHandler = wire[EvalCacheSocketHandler]

  // remote socket support
  Bus.subscribeFun("remoteSocketIn:evalGet") { case TellSriIn(sri, _, msg) =>
    msg obj "d" foreach { d =>
      // TODO send once, let lila-ws distribute
      socketHandler.evalGet(Sri(sri), d, res => Bus.publish(TellSriOut(sri, res), "remoteSocketOut"))
    }
  }
  Bus.subscribeFun("remoteSocketIn:evalPut") { case TellSriIn(sri, Some(userId), msg) =>
    msg obj "d" foreach { d =>
      socketHandler.untrustedEvalPut(Sri(sri), userId, d)
    }
  }
  // END remote socket support

  def cli = new lila.common.Cli {
    def process = { case "eval-cache" :: "drop" :: variantKey :: fenParts =>
      Variant(variantKey).fold(fufail[String]("Invalid variant")) { variant =>
        api.drop(variant, chess.format.FEN(fenParts mkString " ")) inject "done!"
      }
    }
  }
}
