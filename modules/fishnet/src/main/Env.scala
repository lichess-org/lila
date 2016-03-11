package lila.fishnet

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionMove = config getString "collection.move"
  private val CollectionAnalysis = config getString "collection.analysis"
  private val CollectionClient = config getString "collection.client"

  lazy val api = new FishnetApi(
    moveColl = db(CollectionMove),
    analysisColl = db(CollectionAnalysis),
    clientColl = db(CollectionClient))

  def cli = new lila.common.Cli {
    def process = {
      case "fishnet" :: "add" :: "client" :: key :: userId :: skill :: Nil =>
        api.createClient(key, userId, skill) inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "fishnet" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "fishnet")
}
