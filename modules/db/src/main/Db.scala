package lila.db

import reactivemongo.api._
import reactivemongo.api.commands.Command
import scala.annotation.nowarn
import scala.concurrent.duration._
import scala.concurrent.Future

import dsl.Coll
import lila.common.Chronometer
import lila.common.config.CollName

final class AsyncDb(
    name: String,
    uri: String,
    driver: AsyncDriver
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val connection =
    MongoConnection.fromString(uri) flatMap { parsedUri =>
      driver.connect(parsedUri, name.some).dmap(_ -> parsedUri.db)
    }

  private def db: Future[DefaultDB] =
    connection flatMap {
      case (conn, dbName) => conn database dbName.getOrElse("lishogi")
    }

  def apply(name: CollName) = new AsyncColl(() => db.dmap(_(name.value)))
}

final class Db(
    name: String,
    uri: String,
    driver: AsyncDriver
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val logger = lila.db.logger branch name

  private lazy val db: DefaultDB = Chronometer.syncEffect(
    MongoConnection
      .fromString(uri)
      .flatMap { parsedUri =>
        driver
          .connect(parsedUri, name.some)
          .flatMap(_ database parsedUri.db.getOrElse("lishogi"))
      }
      .await(5.seconds, s"db:$name")
  ) { lap =>
    logger.info(s"MongoDB connected to $uri in ${lap.showDuration}")
  }

  def apply(name: CollName): Coll = db(name.value)

  val runCommand = new RunCommand((command, readPreference) => {
    val pack           = reactivemongo.api.bson.collection.BSONSerializationPack
    @nowarn val runner = Command.run(pack, FailoverStrategy.strict)
    runner(db, runner.rawCommand(command)).one[dsl.Bdoc](readPreference)
  })
}
