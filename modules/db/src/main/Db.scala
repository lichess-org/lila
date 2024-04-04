package lila.db

import reactivemongo.api.*

import lila.common.Chronometer
import lila.core.config.CollName
import lila.db.dsl.Coll

final class AsyncDb(
    name: String,
    uri: String,
    driver: AsyncDriver
)(using Executor):

  private lazy val connection: Fu[(MongoConnection, Option[String])] =
    MongoConnection.fromString(uri).flatMap { parsedUri =>
      driver.connect(parsedUri, name.some).dmap(_ -> parsedUri.db)
    }

  private def makeDb: Future[DB] =
    connection.flatMap { case (conn, dbName) =>
      conn.database(dbName.getOrElse("lichess"))
    }

  private val dbCache = new SingleFutureCache[DB](
    compute = () => makeDb,
    expireAfterMillis = 1000
  )

  def apply(name: CollName) = new AsyncColl(name, () => dbCache.get.dmap(_.collection(name.value)))

final class Db(
    name: String,
    uri: String,
    driver: AsyncDriver
)(using Executor):

  private val logger = lila.db.logger.branch(name)

  private lazy val db: DB = Chronometer.syncEffect(
    MongoConnection
      .fromString(uri)
      .flatMap: parsedUri =>
        driver
          .connect(parsedUri, name.some)
          .flatMap(_.database(parsedUri.db.getOrElse("lichess")))
      .await(5.seconds, s"db:$name")
  ) { lap =>
    logger.info(s"MongoDB connected to $uri in ${lap.showDuration}")
  }

  def apply(name: CollName): Coll = db.collection(name.value)
