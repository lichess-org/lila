package lila.search

import com.typesafe.config.Config
import scalastic.elasticsearch
import scala.concurrent.Future
import akka.actor.ActorSystem
import scala.collection.JavaConversions._

final class Env(config: Config, system: ActorSystem) {

  private val ESHost = config getString "es.host"
  private val ESPort = config getInt "es.port"
  private val ESCluster = config getString "es.cluster"
  private val IndexesToOptimize = config getStringList "indexes_to_optimize"

  val esIndexer = elasticsearch.Indexer.transport(
    settings = Map("cluster.name" -> ESCluster),
    host = ESHost,
    ports = Seq(ESPort)
  )

  Future {
    loginfo("[search] Start ElasticSearch")
    esIndexer.start
    loginfo("[search] ElasticSearch is running")
  }

  {
    val scheduler = new lila.common.Scheduler(system)
    import scala.concurrent.duration._

    scheduler.effect(2 hours, "search: optimize index") {
      esIndexer.optimize(IndexesToOptimize)
    }
  }
}

object Env {

  lazy val current = "[boot] search" describes new Env(
    config = lila.common.PlayApp loadConfig "search",
    system = lila.common.PlayApp.system)
}
