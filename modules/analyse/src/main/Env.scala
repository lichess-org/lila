package lila.analyse

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler,
    hub: lila.hub.Env,
    roundSocket: ActorSelection,
    indexer: ActorSelection) {

  private val CollectionAnalysis = config getString "collection.analysis"
  private val CollectionRequester = config getString "collection.requester"
  private val NetDomain = config getString "net.domain"
  private val SocketUidTtl = config duration "socket.uid.ttl"
  private val SocketName = config getString "socket.name"

  lazy val analysisColl = db(CollectionAnalysis)

  lazy val requesterApi = new RequesterApi(db(CollectionRequester))

  lazy val analyser = new Analyser(
    indexer = indexer,
    requesterApi = requesterApi,
    roundSocket = roundSocket,
    bus = system.lilaBus)

  lazy val annotator = new Annotator(NetDomain)

  private val socket = system.actorOf(
    Props(new AnalyseSocket(timeout = SocketUidTtl)), name = SocketName)

  lazy val socketHandler = new AnalyseSocketHandler(socket, hub, evalCacheHandler)
}

object Env {

  lazy val current = "analyse" boot new Env(
    config = lila.common.PlayApp loadConfig "analyse",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    evalCacheHandler = lila.evalCache.Env.current.socketHandler,
    hub = lila.hub.Env.current,
    roundSocket = lila.hub.Env.current.socket.round,
    indexer = lila.hub.Env.current.actor.gameSearch)
}
