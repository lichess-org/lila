package lila.ai

import scala.collection.JavaConversions._

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.{ Config => TypesafeConfig }

import lila.common.PimpedConfig._

final class Env(
    c: TypesafeConfig,
    uciMemo: lila.game.UciMemo,
    db: lila.db.Env,
    system: ActorSystem) {

  private val settings = new {
    val Endpoint = c getString "endpoint"
    val CallbackUrl = c getString "callback_url"
    val ActorName = c getString "actor.name"
    val CollectionAiPerf = c getString "collection.ai_perf"
    val AiPerfCacheTtl = c duration "ai_perf.cache_ttl"
  }
  import settings._

  val ServerOnly = c getBoolean "server-only"

  private val config = new Config(
    execPath = c getString "exec_path",
    hashSize = c getInt "hash_size",
    nbThreads = c getInt "threads",
    nbInstances = c getInt "instances",
    playMaxMoveTime = c duration "play.movetime",
    analyseMoveTime = c duration "analyse.movetime",
    playTimeout = c duration "play.timeout",
    analyseMaxPlies = c getInt "analyse.max_plies",
    debug = c getBoolean "debug")

  lazy val aiPerfApi = new AiPerfApi(db(CollectionAiPerf), AiPerfCacheTtl)

  def ratingOf(level: Int) = aiPerfApi.intRatings get level

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.ai.Analyse(gameId, uciMoves, fen, requestedByHuman) =>
        client.analyse(gameId, uciMoves, fen, requestedByHuman)
    }
  }), name = ActorName)

  lazy val client = new Client(
    config = config,
    endpoint = Endpoint,
    callbackUrl = CallbackUrl,
    uciMemo = uciMemo)

  lazy val server = new Server(
    config = config,
    queue = system.actorOf(Props(new Queue(config))),
    uciMemo = uciMemo)
}

object Env {

  lazy val current = "[boot] ai" describes new Env(
    c = lila.common.PlayApp loadConfig "ai",
    uciMemo = lila.game.Env.current.uciMemo,
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system)
}
