package lila.opening

import com.softwaremill.macwire._
import com.softwaremill.tagging._
import scala.concurrent.duration._

import lila.common.config
import lila.memo.{ CacheApi, MongoCache }
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

@Module
final class Env(
    db: lila.db.Db,
    cacheApi: CacheApi,
    mongoCache: MongoCache.Api,
    appConfig: Configuration,
    cookieBaker: lila.common.LilaCookie,
    ws: StandaloneWSClient
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler,
    mat: akka.stream.Materializer
) {

  private val explorerEndpoint = appConfig.get[String]("explorer.endpoint").taggedWith[ExplorerEndpoint]

  private lazy val explorer = wire[OpeningExplorer]

  lazy val config = wire[OpeningConfigStore]

  lazy val api = wire[OpeningApi]
}

trait ExplorerEndpoint
