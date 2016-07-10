package lila.explorer

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    gameColl: lila.db.dsl.Coll,
    system: ActorSystem) {

  private val Endpoint = config getString "endpoint"
  private val InternalEndpoint = config getString "internal_endpoint"
  private val IndexFlow = config getBoolean "index_flow"

  private lazy val indexer = new ExplorerIndexer(
    gameColl = gameColl,
    internalEndpoint = InternalEndpoint)

  def cli = new lila.common.Cli {
    def process = {
      case "explorer" :: "index" :: since :: Nil => indexer(since) inject "done"
    }
  }

  def fetchPgn(id: String): Fu[Option[String]] = {
    import play.api.libs.ws.WS
    import play.api.Play.current
    WS.url(s"$InternalEndpoint/master/pgn/$id").get() map {
      case res if res.status == 200 => res.body.some
      case _                        => None
    }
  }

  if (IndexFlow) system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted => indexer(game)
    }
  })), 'finishGame)
}

object Env {

  lazy val current = "explorer" boot new Env(
    config = lila.common.PlayApp loadConfig "explorer",
    gameColl = lila.game.Env.current.gameColl,
    system = lila.common.PlayApp.system)
}
