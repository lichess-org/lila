package lila.search

import com.typesafe.config.Config
import scalastic.elasticsearch
import scala.concurrent.Future

final class Env(config: Config) {

  private val ESHost = config getString "es.host"
  private val ESPort = config getInt "es.port"
  private val ESCluster = config getString "es.cluster"

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
}

object Env {

  lazy val current = "[boot] search" describes new Env(
    config = lila.common.PlayApp loadConfig "search")
}
