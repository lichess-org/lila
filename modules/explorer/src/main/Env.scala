package lila.explorer

import com.typesafe.config.Config

final class Env(config: Config) {

  private val Endpoint = config getString "endpoint"

  private lazy val indexer = new ExplorerIndexer(endpoint = Endpoint)

  def cli = new lila.common.Cli {
    def process = {
      case "explorer" :: "index" :: variant :: Nil => indexer(variant) inject "done"
    }
  }
}

object Env {

  lazy val current = "explorer" boot new Env(
    config = lila.common.PlayApp loadConfig "explorer")
}
