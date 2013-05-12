package lila.search

import com.typesafe.config.Config
import scalastic.elasticsearch
import scala.concurrent.Future
import akka.actor.ActorSystem
import scala.collection.JavaConversions._

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val ESHost = config getString "es.host"
  private val ESPort = config getInt "es.port"
  private val ESCluster = config getString "es.cluster"
  private val IndexesToOptimize = config getStringList "indexes_to_optimize"

  val esIndexer = "[search] Instanciate indexer" describes elasticsearch.Indexer.transport(
    settings = Map("cluster.name" -> ESCluster),
    host = ESHost,
    ports = Seq(ESPort))

  {
    import scala.concurrent.duration._

    scheduler.effect(2 hours, "search: optimize index") {
      esIndexer.optimize(IndexesToOptimize)
    }

    scheduler.once(1 second) {
      loginfo("[search] Start ElasticSearch")
      esIndexer.start
      loginfo("[search] ElasticSearch is running")
    }
  }
}

object Env {

  lazy val current = "[boot] search" describes new Env(
    config = lila.common.PlayApp loadConfig "search",
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
