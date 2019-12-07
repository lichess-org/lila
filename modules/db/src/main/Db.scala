package lila.db

import play.api.inject.ApplicationLifecycle
import reactivemongo.api._
import reactivemongo.api.commands.Command
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.util.Failure

import dsl.Coll
import lila.common.Chronometer
import lila.common.config.CollName

final class AsyncDb(
    name: String,
    uri: MongoConnection.ParsedURI,
    driver: MongoDriver
) {

  private val dbName = uri.db | "lichess"

  lazy val connection = Future fromTry driver.connection(uri, name.some, true)

  private def db = connection.flatMap(_ database dbName)

  def apply(name: CollName) = new AsyncColl(() => db.dmap(_(name.value)))
}

final class Db(
    name: String,
    uri: MongoConnection.ParsedURI,
    driver: MongoDriver
) {

  private val logger = lila.db.logger branch name

  private val dbName = uri.db | "lichess"

  lazy val connection = driver.connection(uri, name.some, true).get

  private lazy val db = Chronometer.syncEffect(
    Await.result(connection database dbName, 5.seconds)
  ) { lap =>
      logger.info(s"MongoDB connected to $dbName in ${lap.showDuration}")
    }

  def apply(name: CollName): Coll = db(name.value)

  val runCommand = new RunCommand((command, readPreference) => {
    val pack = reactivemongo.api.bson.collection.BSONSerializationPack
    val runner = Command.run(pack, FailoverStrategy.strict)
    runner(db, runner.rawCommand(command)).one[dsl.Bdoc](readPreference)
  })
}
