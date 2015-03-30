package lila.simul

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.hub.actorApi.map.Ask
import lila.hub.{ ActorMap, Sequencer }
import lila.socket.actorApi.GetVersion
import lila.socket.History
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    flood: lila.security.Flood,
    hub: lila.hub.Env,
    roundMap: ActorRef,
    lightUser: String => Option[lila.common.LightUser],
    isOnline: String => Boolean) {

  private val settings = new {
    val CollectionSimul = config getString "collection.simul"
  }
  import settings._

  lazy val api = new SimulApi(
    simulColl = simulColl)

  lazy val forms = new DataForm

  private[simul] val simulColl = db(CollectionSimul)
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "[boot] simul" describes new Env(
    config = lila.common.PlayApp loadConfig "simul",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    mongoCache = lila.memo.Env.current.mongoCache,
    flood = lila.security.Env.current.flood,
    hub = lila.hub.Env.current,
    roundMap = lila.round.Env.current.roundMap,
    lightUser = lila.user.Env.current.lightUser,
    isOnline = lila.user.Env.current.isOnline)
}
