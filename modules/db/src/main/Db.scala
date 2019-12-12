package lila.db

import com.github.ghik.silencer.silent
import reactivemongo.api._
import reactivemongo.api.commands.Command
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }

import dsl.Coll
import lila.common.Chronometer
import lila.common.config.CollName

final class AsyncDb(
    name: String,
    uri: MongoConnection.ParsedURI,
    driver: AsyncDriver
) {

  private val dbName = uri.db | "lichess"

  lazy val connection: Future[MongoConnection] = driver.connect(uri, name.some)

  private def db: Future[DefaultDB] = connection.flatMap(_ database dbName)

  def apply(name: CollName) = new AsyncColl(() => db.dmap(_(name.value)))
}

final class Db(
    name: String,
    uri: MongoConnection.ParsedURI,
    driver: AsyncDriver
) {

  private val logger = lila.db.logger branch name

  private val dbName = uri.db | "lichess"

  private lazy val db: DefaultDB = Chronometer.syncEffect(
    Await.result(
      driver.connect(uri, name.some).flatMap(_ database dbName),
      5.seconds
    )
  ) { lap =>
      logger.info(s"MongoDB connected to $dbName in ${lap.showDuration}")
    }

  def apply(name: CollName): Coll = db(name.value)

  val runCommand = new RunCommand((command, readPreference) => {
    val pack = reactivemongo.api.bson.collection.BSONSerializationPack
    @silent val runner = Command.run(pack, FailoverStrategy.strict)
    runner(db, runner.rawCommand(command)).one[dsl.Bdoc](readPreference)
  })
}
