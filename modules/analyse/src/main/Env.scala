package lidraughts.analyse

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    system: ActorSystem,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler,
    hub: lidraughts.hub.Env,
    indexer: ActorSelection
) {

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
    roundSocket = hub.socket.round,
    studyActor = hub.actor.study,
    bus = system.lidraughtsBus
  )

  lazy val annotator = new Annotator(NetDomain)

  private val socket = system.actorOf(
    Props(new AnalyseSocket(timeout = SocketUidTtl)), name = SocketName
  )

  lazy val socketHandler = new AnalyseSocketHandler(socket, hub, evalCacheHandler)
}

object Env {

  lazy val current = "analyse" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "analyse",
    db = lidraughts.db.Env.current,
    system = lidraughts.common.PlayApp.system,
    evalCacheHandler = lidraughts.evalCache.Env.current.socketHandler,
    hub = lidraughts.hub.Env.current,
    indexer = lidraughts.hub.Env.current.actor.gameSearch
  )
}
