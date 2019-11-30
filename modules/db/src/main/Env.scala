package lila.db

import io.methvin.play.autoconfig._
import play.api.Configuration
import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.commands.Command
import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver, FailoverStrategy, ReadPreference }
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success }

import dsl.Coll
import lila.common.{ CollName, Chronometer }
import lila.common.config._

case class DbConfig(
    uri: String,
    @ConfigName("image.collection") imageCollName: Option[CollName] = None
)

final class Env(name: String, config: DbConfig) {

  private val driver = MongoDriver()
  private val parsedUri = MongoConnection.parseURI(config.uri).get
  // private val connection = Future.fromTry(parsedUri.flatMap(driver.connection(_, true)))
  private val dbName = parsedUri.db | "lichess"
  val conn = driver.connection(parsedUri, name.some, true).get
  private val db = Chronometer.syncEffect(
    Await.result(conn database dbName, 3.seconds)
  ) { lap =>
      logger.info(s"$name MongoDB connected to $dbName in ${lap.showDuration}")
    }
  //#TODO add lifecycle?

  def apply(name: CollName): Coll = db(name.value)

  // val runCommand: RunCommand = (command, readPreference) => {
  //   val runner = Command.run(BSONSerializationPack, FailoverStrategy.strict)
  //   runner(db, runner.rawCommand(command)).one[dsl.Bdoc](readPreference)
  // }

  object image {
    private lazy val imageColl = config.imageCollName map apply
    import dsl._
    // import DbImage.DbImageBSONHandler
    // def fetch(id: String): Fu[Option[DbImage]] = imageColl ?? {
    //   _.byId[DbImage](id)
    // }
  }
}

object Env {

  implicit val configLoader = AutoConfig.loader[DbConfig]

  def main(appConfig: Configuration) = new Env(
    name = "main",
    config = appConfig.get[DbConfig]("mongodb")
  )
}
