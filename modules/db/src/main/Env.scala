package lila.db

import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import reactivemongo.api._
import reactivemongo.api.commands.Command
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

import dsl.Coll
import lila.common.Chronometer
import lila.common.config._

final class DbConfig(
    val uri: String
)

final class Env(
    name: String,
    config: DbConfig,
    lifecycle: ApplicationLifecycle
) {

  private val logger = lila.db.logger branch name
  private lazy val driver = {
    println(s"########################################### $name driver")
    new MongoDriver()
  }
  private lazy val parsedUri = MongoConnection.parseURI(config.uri).get
  // private val connection = Future.fromTry(parsedUri.flatMap(driver.connection(_, true)))
  private lazy val dbName = parsedUri.db | "lichess"
  val conn = driver.connection(parsedUri, name.some, true).get
  registerDriverShutdownHook(driver, conn)
  private lazy val db = Chronometer.syncEffect(
    Await.result(conn database dbName, 3.seconds)
  ) { lap =>
      logger.info(s"MongoDB connected to $dbName in ${lap.showDuration}")
    }

  def apply(name: CollName): Coll = db(name.value)

  val runCommand = new RunCommand((command, readPreference) => {
    val pack = reactivemongo.api.bson.collection.BSONSerializationPack
    val runner = Command.run(pack, FailoverStrategy.strict)
    runner(db, runner.rawCommand(command)).one[dsl.Bdoc](readPreference)
  })

  object image {
    private lazy val imageColl = apply(CollName("image"))
    import dsl._
    import DbImage.DbImageBSONHandler
    def fetch(id: String): Fu[Option[DbImage]] = imageColl.byId[DbImage](id)
  }

  private def registerDriverShutdownHook(mongoDriver: MongoDriver, connection: MongoConnection) =
    lifecycle.addStopHook { () =>
      logger.info("ReactiveMongoApi stopping...")
      Await.ready(connection.askClose()(5.seconds).map { _ =>
        logger.info("MongoDB connection closed")
      }.andThen {
        case Failure(reason) =>
          logger.error(s"MongoDB connection didn't close: $reason")
          reason.printStackTrace()
          mongoDriver.close() // Close anyway
        case _ => mongoDriver.close()
      }, 6.seconds)
    }
}

object Env {

  implicit val configLoader = AutoConfig.loader[DbConfig]

  def main(appConfig: Configuration, lifecycle: ApplicationLifecycle) = new Env(
    name = "main",
    config = appConfig.get[DbConfig]("mongodb"),
    lifecycle = lifecycle
  )
}
