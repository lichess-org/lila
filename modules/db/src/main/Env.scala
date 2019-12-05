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

  private val driver = MongoDriver()
  private val parsedUri = MongoConnection.parseURI(config.uri).get
  // private val connection = Future.fromTry(parsedUri.flatMap(driver.connection(_, true)))
  private val dbName = parsedUri.db | "lichess"
  val conn = driver.connection(parsedUri, name.some, true).get
  registerDriverShutdownHook(driver, conn)
  private val db = Chronometer.syncEffect(
    Await.result(conn database dbName, 3.seconds)
  ) { lap =>
      logger.info(s"$name MongoDB connected to $dbName in ${lap.showDuration}")
    }
  //#TODO add lifecycle?

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

  private def registerDriverShutdownHook(
    mongoDriver: MongoDriver,
    connection: MongoConnection
  ): Unit = lifecycle.addStopHook { () =>
    logger.info("ReactiveMongoApi stopping...")

    Await.ready(connection.askClose()(10.seconds).map { _ =>
      logger.info("ReactiveMongoApi connections are stopped")
    }.andThen {
      case Failure(reason) =>
        reason.printStackTrace()
        mongoDriver.close() // Close anyway

      case _ => mongoDriver.close()
    }, 12.seconds)
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
