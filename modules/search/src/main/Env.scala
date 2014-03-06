package lila.search

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.{ Success, Failure }

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import org.elasticsearch.common.settings.ImmutableSettings

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val ESHost = config getString "es.host"
  private val ESPort = config getInt "es.port"
  private val ESCluster = config getString "es.cluster"
  private val IndexesToOptimize = config getStringList "indexes_to_optimize"
  private val IndexerMaxAttempts = 10

  lazy val client = {
    val settings = ImmutableSettings.settingsBuilder()
      .put("cluster.name", ESCluster).build()
    ElasticClient.remote(settings, ESHost -> ESPort)
  }

  {
    import scala.concurrent.duration._
    import com.sksamuel.elastic4s.ElasticDsl._
    scheduler.effect(1 hour, "search: optimize index") {
      client execute {
        optimize index IndexesToOptimize
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
