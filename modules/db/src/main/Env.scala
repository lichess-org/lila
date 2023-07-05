package lila.db

import akka.actor.CoordinatedShutdown
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import com.typesafe.config.Config
import play.api.Configuration
import reactivemongo.api.*

import lila.common.Lilakka

// weakly replicated DB for low value documents
trait YoloDb

@Module
final class Env(
    appConfig: Configuration,
    shutdown: CoordinatedShutdown
)(using Executor):

  private val driver = new AsyncDriver(appConfig.get[Config]("mongodb").some)

  lazy val mainDb = Db(
    name = "main",
    uri = appConfig.get[String]("mongodb.uri"),
    driver = driver
  )

  lazy val yoloDb = AsyncDb(
    name = "yolo",
    uri = appConfig.get[String]("mongodb.yolo.uri"),
    driver = driver
  ).taggedWith[YoloDb]

  def asyncDb(name: String, uri: String) =
    AsyncDb(
      name = name,
      uri = uri,
      driver = driver
    )

  Lilakka.shutdown(shutdown, _.PhaseServiceStop, "Closing mongodb driver"): () =>
    driver.close()
