package lila.db

import scala.concurrent.ExecutionContext

import play.api.Configuration

import akka.actor.CoordinatedShutdown
import com.typesafe.config.Config
import reactivemongo.api._

import lila.common.Lilakka

final class Env(
    appConfig: Configuration,
    shutdown: CoordinatedShutdown,
)(implicit ec: ExecutionContext) {

  private val driver = new AsyncDriver(appConfig.get[Config]("mongodb").some)

  def asyncDb(name: String, uri: String) =
    new AsyncDb(
      name = name,
      uri = uri,
      driver = driver,
    )

  def blockingDb(name: String, uri: String) =
    new Db(
      name = name,
      uri = uri,
      driver = driver,
    )

  Lilakka.shutdown(shutdown, _.PhaseServiceStop, "Closing mongodb driver") { () =>
    driver.close()
  }
}
