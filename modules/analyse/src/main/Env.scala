package lila.analyse

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler,
    hub: lila.hub.Env,
    indexer: ActorSelection
) {

  private val CollectionAnalysis = config getString "collection.analysis"
  private val CollectionRequester = config getString "collection.requester"
  private val NetDomain = config getString "net.domain"
  private val SocketUidTtl = config duration "socket.uid.ttl"

  lazy val analysisColl = db(CollectionAnalysis)

  lazy val requesterApi = new RequesterApi(db(CollectionRequester))

  lazy val analyser = new Analyser(
    indexer = indexer,
    requesterApi = requesterApi,
    bus = system.lilaBus
  )

  lazy val annotator = new Annotator(NetDomain)

  private val socket = new AnalyseSocket(system, SocketUidTtl)

  lazy val socketHandler = new AnalyseSocketHandler(socket, hub, evalCacheHandler)
}

object Env {

  lazy val current = "analyse" boot new Env(
    config = lila.common.PlayApp loadConfig "analyse",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    evalCacheHandler = lila.evalCache.Env.current.socketHandler,
    hub = lila.hub.Env.current,
    indexer = lila.hub.Env.current.gameSearch
  )
}
