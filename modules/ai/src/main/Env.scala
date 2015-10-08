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
  }
  import settings._

  val ServerOnly = c getBoolean "server-only"

  private val config = new Config(
    command = c.getStringList("command").toList,
    hashSize = c getInt "hash_size",
    nbThreads = c getInt "threads",
    nbInstances = c getInt "instances",
    playMaxMoveTime = c duration "play.movetime",
    analyseMoveTime = c duration "analyse.movetime",
    playTimeout = c duration "play.timeout",
    analyseMaxPlies = c getInt "analyse.max_plies",
    debug = c getBoolean "debug")

  lazy val aiPerfApi = new AiPerfApi

  def ratingOf(level: Int) = aiPerfApi.intRatings get level

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.ai.Analyse(gameId, uciMoves, fen, requestedByHuman, variant) =>
        client.analyse(gameId, uciMoves, fen, requestedByHuman, actorApi.Variant(variant))
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

  lazy val current = "ai" boot new Env(
    c = lila.common.PlayApp loadConfig "ai",
    uciMemo = lila.game.Env.current.uciMemo,
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system)
}
