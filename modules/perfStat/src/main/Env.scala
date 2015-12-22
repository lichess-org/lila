package lila.perfStat

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import akka.actor._
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionPerfStat = config getString "collection.perf_stat"
  }
  import settings._

  lazy val api = new PerfStatApi(
    coll = db(CollectionPerfStat))

  // system.actorOf(Props(new Actor {
  //   system.lilaBus.subscribe(self, 'analysisReady)
  //   def receive = {
  //     case lila.analyse.actorApi.AnalysisReady(game, _) => api updateGame game
  //   }
  // }))
}

object Env {

  lazy val current: Env = "perfStat" boot new Env(
    config = lila.common.PlayApp loadConfig "perfStat",
    db = lila.db.Env.current)
}
