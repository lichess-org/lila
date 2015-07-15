package lila.db

import com.typesafe.config.Config
import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import scala.util.{ Success, Failure }
import Types._

final class Env(
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle) {

  lazy val db = {
    val parsedUri: MongoConnection.ParsedURI =
      MongoConnection.parseURI(config.getString("uri")) match {
        case Success(parsedURI) => parsedURI
        case Failure(e)         => sys error s"Invalid mongodb.uri"
      }
    val driver = new MongoDriver(Some(config))
    val connection = driver.connection(parsedUri)

    parsedUri.db.fold[DefaultDB](sys error s"cannot resolve database from URI: $parsedUri") { dbUri =>
      val db = DB(dbUri, connection)
      registerDriverShutdownHook(connection, driver)
      loginfo(s"""ReactiveMongoApi successfully started with DB '$dbUri'! Servers:\n\t\t${parsedUri.hosts.map { s => s"[${s._1}:${s._2}]" }.mkString("\n\t\t")}""")
      db
    }
  }

  def apply(name: String): Coll = db(name)

  private def registerDriverShutdownHook(connection: MongoConnection, mongoDriver: MongoDriver): Unit =
    lifecycle.addStopHook { () =>
      Future {
        loginfo("ReactiveMongoApi stopping...")
        val f = connection.askClose()(10.seconds)
        f.onComplete {
          case e => loginfo(s"ReactiveMongoApi connections stopped. [$e]")
        }
        Await.ready(f, 10.seconds)
        mongoDriver.close()
      }
    }
}

object Env {

  lazy val current = "[boot] db" describes new Env(
    config = lila.common.PlayApp loadConfig "mongodb",
    lifecycle = lila.common.PlayApp.lifecycle)
}
