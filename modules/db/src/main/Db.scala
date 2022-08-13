package lila.db

import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.Future

import lila.common.Chronometer
import lila.common.config.CollName
import lila.db.dsl.Coll

final class AsyncDb(
    name: String,
    uri: String,
    driver: AsyncDriver
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val connection: Fu[(MongoConnection, Option[String])] =
    MongoConnection.fromString(uri) flatMap { parsedUri =>
      driver.connect(parsedUri, name.some).dmap(_ -> parsedUri.db)
    }

  private def makeDb: Future[DB] =
    connection flatMap { case (conn, dbName) =>
      conn database dbName.getOrElse("lichess")
    }

  private val dbCache = new SingleFutureCache[DB](
    compute = () => makeDb,
    expireAfterMillis = 1000
  )

  def apply(name: CollName) = new AsyncColl(name, () => dbCache.get.dmap(_.collection(name.value)))
}

// apologies for the devMode flag, but maybe this could help nasty errors on prod startup as well?
// just want to get rid of the 20-30 red mongo errors in startup logs so as not to become desensitized
final class Db(
    name: String,
    uri: String,
    driver: AsyncDriver,
    devMode: Boolean = false
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val logger = lila.db.logger branch name

  private lazy val db: DB = Chronometer.syncEffect(
    MongoConnection
      .fromString(uri)
      .flatMap { parsedUri =>
        if (devMode)
          driver
            .connect(
              parsedUri.hosts.map(_._1).toSeq,
              MongoConnectionOptions.default.copy(failoverStrategy = FailoverStrategy(1.second, 5, _ * 1.5)),
              name
            )
            .flatMap(_ database parsedUri.db.getOrElse("lichess"))
        else
          driver.connect(parsedUri, name.some).flatMap(_ database parsedUri.db.getOrElse("lichess"))
      }
      .await(5.seconds, s"db:$name")
  ) { lap =>
    logger.info(s"MongoDB connected to $uri in ${lap.showDuration}")
  }

  def apply(name: CollName): Coll = db.collection(name.value)
}
