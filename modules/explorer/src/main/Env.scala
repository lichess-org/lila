package lidraughts.explorer

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    gameColl: lidraughts.db.dsl.Coll,
    gameImporter: lidraughts.importer.Importer,
    settingStore: lidraughts.memo.SettingStore.Builder,
    system: ActorSystem
) {

  private val InternalEndpoint = config getString "internal_endpoint"

  private lazy val indexer = new ExplorerIndexer(
    gameColl = gameColl,
    internalEndpoint = InternalEndpoint
  )

  lazy val importer = new ExplorerImporter(
    endpoint = InternalEndpoint,
    gameImporter = gameImporter
  )

  def cli = new lidraughts.common.Cli {
    def process = {
      case "explorer" :: "index" :: since :: Nil => indexer(since) inject "done"
    }
  }

  lazy val indexFlowSetting = settingStore[Boolean](
    "explorerIndexFlow",
    default = true,
    text = "Explorer: index new games as soon as they complete".some
  )

  system.lidraughtsBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.game.actorApi.FinishGame(game, _, _) if !game.aborted && indexFlowSetting.get() => indexer(game)
    }
  })), 'finishGame)
}

object Env {

  lazy val current = "explorer" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "explorer",
    gameColl = lidraughts.game.Env.current.gameColl,
    gameImporter = lidraughts.importer.Env.current.importer,
    settingStore = lidraughts.memo.Env.current.settingStore,
    system = lidraughts.common.PlayApp.system
  )
}
