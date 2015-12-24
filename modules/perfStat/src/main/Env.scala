package lila.perfStat

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import akka.actor._
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionPerfStat = config getString "collection.perf_stat"
  }
  import settings._

  lazy val storage = new PerfStatStorage(
    coll = db(CollectionPerfStat))

  lazy val indexer = new PerfStatIndexer(
    storage = storage,
    sequencer = system.actorOf(Props(classOf[lila.hub.Sequencer], None, None)))

  def get(user: lila.user.User, perfType: lila.rating.PerfType) =
    storage.find(user.id, perfType) orElse {
      indexer.userPerf(user, perfType) >> storage.find(user.id, perfType)
    } map (_ | PerfStat.init(user.id, perfType))

  system.actorOf(Props(new Actor {
    context.system.lilaBus.subscribe(self, 'finishGame)
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) => indexer addGame game
    }
  }))
}

object Env {

  lazy val current: Env = "perfStat" boot new Env(
    config = lila.common.PlayApp loadConfig "perfStat",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current)
}
