package lila.explorer

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    gameColl: lila.db.dsl.Coll,
    gameImporter: lila.importer.Importer,
    system: ActorSystem
) {

  private val InternalEndpoint = config getString "internal_endpoint"
  private val IndexFlow = config getBoolean "index_flow"

  private lazy val indexer = new ExplorerIndexer(
    gameColl = gameColl,
    internalEndpoint = InternalEndpoint
  )

  lazy val importer = new ExplorerImporter(
    endpoint = InternalEndpoint,
    gameImporter = gameImporter
  )

  def cli = new lila.common.Cli {
    def process = {
      case "explorer" :: "index" :: since :: Nil => indexer(since) inject "done"
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
    gameImporter = lila.importer.Env.current.importer,
    system = lila.common.PlayApp.system
  )
}
