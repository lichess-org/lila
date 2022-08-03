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
    ws: StandaloneWSClient
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler,
    mat: akka.stream.Materializer
) {

  private val explorerEndpoint = appConfig.get[String]("explorer.endpoint").taggedWith[ExplorerEndpoint]

  private val openingColl = db(config.CollName("opening")).taggedWith[OpeningColl]

  private lazy val update: OpeningUpdate = wire[OpeningUpdate]

  private val allGamesHistory = cacheApi
    .unit { _.refreshAfterWrite(1 hour).buildAsyncFuture { _ => update.fetchHistory(none) } }
    .taggedWith[AllGamesHistory]

  lazy val api = wire[OpeningApi]

  // api.updateOpenings.logFailure(logger).unit
  scheduler.scheduleAtFixedRate(5 minutes, 7 day) { () =>
    update.all.unit
  }
}

trait ExplorerEndpoint
trait OpeningColl
trait AllGamesHistory
