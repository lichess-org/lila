package lila.db

import akka.actor.Scheduler
import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.Future

import lila.common.Chronometer
import lila.common.config.CollName
import lila.db.dsl.Coll
import reactivemongo.core.nodeset.NodeInfo

final class AsyncDb(
    name: String,
    uri: String,
    driver: AsyncDriver
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val connection =
    MongoConnection.fromString(uri) flatMap { parsedUri =>
      driver.connect(parsedUri, name.some).dmap(_ -> parsedUri.db)
    }

  private def db: Future[DB] =
    connection flatMap { case (conn, dbName) =>
      conn database dbName.getOrElse("lichess")
    }

  def apply(name: CollName) = new AsyncColl(name, () => db.dmap(_(name.value)))
}

final class Db(
    name: String,
    uri: String,
    driver: AsyncDriver
)(implicit ec: scala.concurrent.ExecutionContext, scheduler: Scheduler) {

  private val logger = lila.db.logger branch name

  private lazy val parsedUri: MongoConnection.URI[Option[String]] =
    MongoConnection.fromString(uri).await(3 seconds, s"db:$name parsedUri")

  private lazy val connection: Future[MongoConnection] =
    driver.connect(parsedUri, name.some)

  private lazy val db: DB = Chronometer.syncEffect(
    connection
      .flatMap(_ database parsedUri.db.getOrElse("lichess"))
      .await(5.seconds, s"db:$name")
  ) { lap =>
    logger.info(s"MongoDB connected to $uri in ${lap.showDuration}")
  }

  scheduler.scheduleWithFixedDelay(5 seconds, 5 seconds) { () =>
    connection.flatMap(_.askNodeSetInfo(1 second)) foreach { info =>
      import info._
      def nodeInfo(n: NodeInfo) = s"""
          |name: ${n.name}
          |aliases: ${n.aliases}
          |host: ${n.host}
          |port: ${n.port}
          |status: ${n.status}
          |connections: ${n.connections}
          |connected: ${n.connected}
          |authenticated: ${n.authenticated}
          |tags: ${n.tags}
          |protocolMetadata: ${n.protocolMetadata}
          |pingInfo: ${n.pingInfo}""".stripMargin.linesIterator.map(l => s"  $l").mkString("\n")
      println(s"""
        |name: ${info.name}
        |version: $version
        |nodes: ${nodes.map(nodeInfo).mkString("\n\n")}
        |primary: $primary
        |mongos: $mongos
        |secs: $secondaries
        |nearest: $nearest
        |awaitingRequests: $awaitingRequests
        |maxAwaitingRequestsPerChannel: $maxAwaitingRequestsPerChannel""".stripMargin)
    }
  }

  def apply(name: CollName): Coll = db(name.value)
}
