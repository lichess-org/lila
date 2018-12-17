package lila.db

import com.typesafe.config.Config
import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.commands.Command
import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver, FailoverStrategy, ReadPreference }
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success }

import dsl.Coll
import lila.common.Chronometer

final class Env(
    name: String,
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private lazy val (connection, dbName) = {

    val driver = MongoDriver(config)

    registerDriverShutdownHook(driver)

    (for {
      parsedUri <- MongoConnection.parseURI(config getString "uri")
      con <- driver.connection(parsedUri, true)
      db <- parsedUri.db match {
        case Some(name) => Success(name)
        case _ => Failure[String](new IllegalArgumentException(
          s"cannot resolve connection from URI: $parsedUri"
        ))
      }
    } yield con -> db).get
  }

  private lazy val lnm = s"$name ${connection.supervisor}/${connection.name}"

  private lazy val db =
    Chronometer.syncEffect(Await.result(connection database dbName, 5.seconds)) { lap =>
      logger.info(s"$lnm MongoDB connected in ${lap.showDuration}")
    }

  def apply(name: String): Coll = db(name)

  val runCommand: RunCommand = (command, readPreference) => {
    val runner = Command.run(BSONSerializationPack, FailoverStrategy.strict)
    runner(db, runner.rawCommand(command)).one[dsl.Bdoc](readPreference)
  }

  object image {
    private lazy val imageColl = apply(config getString "image.collection")
    import dsl._
    import DbImage.DbImageBSONHandler
    def fetch(id: String): Fu[Option[DbImage]] = imageColl.byId[DbImage](id)
  }

  private def registerDriverShutdownHook(mongoDriver: MongoDriver): Unit =
    lifecycle.addStopHook { () =>
      logger.info(s"$lnm Stopping the MongoDriver...")
      Future(mongoDriver.close())
    }
}

object Env {

  lazy val current = "db" boot new Env(
    name = "main",
    config = lila.common.PlayApp loadConfig "mongodb",
    lifecycle = lila.common.PlayApp.lifecycle
  )
}
