package lila.search

import scala.collection.JavaConversions._
import scala.concurrent.Future

import akka.actor.ActorSystem
import com.typesafe.config.Config
import scalastic.elasticsearch

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val ESHost = config getString "es.host"
  private val ESPort = config getInt "es.port"
  private val ESCluster = config getString "es.cluster"
  private val IndexesToOptimize = config getStringList "indexes_to_optimize"

  val esIndexer = Future {
    "[search] Instanciate indexer" describes elasticsearch.Indexer.transport(
      settings = Map("cluster.name" -> ESCluster),
      host = ESHost,
      ports = Seq(ESPort))
  } andThen {
    case scala.util.Success(indexer) ⇒
      loginfo("[search] Start indexer")
      indexer.start
      loginfo("[search] Indexer is running")
  }

  {
    import scala.concurrent.duration._

    scheduler.effect(1 hour, "search: optimize index") {
      esIndexer foreach { es ⇒
        try {
          es optimize IndexesToOptimize
        }
        catch {
          case e: org.elasticsearch.indices.IndexMissingException ⇒
            play.api.Logger("search").warn(e.toString)
        }
      }
    }
  }
}

object Env {

  lazy val current = "[boot] search" describes new Env(
    config = lila.common.PlayApp loadConfig "search",
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
