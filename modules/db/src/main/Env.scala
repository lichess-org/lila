package lila.db

import com.typesafe.config.Config
import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, ConfigLoader }
import reactivemongo.api._

final class Env(
    appConfig: Configuration,
    lifecycle: ApplicationLifecycle
) {

  private lazy val driver = new MongoDriver(appConfig.get[Config]("mongodb").some)

  def asyncDb(name: String, uri: MongoConnection.ParsedURI) = new AsyncDb(
    name = name,
    uri = uri,
    driver = driver
  )

  def blockingDb(name: String, uri: MongoConnection.ParsedURI) = new Db(
    name = name,
    uri = uri,
    driver = driver
  )

  lifecycle.addStopHook { () =>
    scala.concurrent.Future(driver.close())
  }
}

object DbConfig {

  implicit val uriLoader = ConfigLoader { c => k =>
    MongoConnection.parseURI(c.getString(k)).get
  }
}
