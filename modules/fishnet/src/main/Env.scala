package lila.fishnet

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem) {

  private val CollectionMove = config getString "collection.move"
  private val CollectionAnalysis = config getString "collection.analysis"
  private val CollectionClient = config getString "collection.client"

  lazy val api = new FishnetApi(
    moveColl = db(CollectionMove),
    analysisColl = db(CollectionAnalysis),
    clientColl = db(CollectionClient),
    sequencer = sequencer)

  private lazy val sequencer = new lila.hub.FutureSequencer(
    system = system,
    receiveTimeout = None,
    executionTimeout = Some(1 second))

  def cli = new lila.common.Cli {
    def process = {
      case "fishnet" :: "add" :: "client" :: key :: userId :: skill :: Nil =>
        api.createClient(key, userId, skill) inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "fishnet" boot new Env(
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "fishnet")
}
