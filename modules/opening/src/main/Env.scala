package lila.opening

import com.softwaremill.macwire._
import com.softwaremill.tagging._
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._

import lila.common.config.CollName
import lila.game.{ GameRepo, PgnDump }
import lila.memo.{ CacheApi, MongoCache }

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: GameRepo,
    pgnDump: PgnDump,
    cacheApi: CacheApi,
    appConfig: Configuration,
    cookieBaker: lila.common.LilaCookie,
    ws: StandaloneWSClient
)(implicit ec: scala.concurrent.ExecutionContext, scheduler: akka.actor.Scheduler) {

  private val explorerEndpoint = appConfig.get[String]("explorer.endpoint").taggedWith[ExplorerEndpoint]
  private lazy val wikiColl    = db(CollName("opening_wiki")).taggedWith[WikiColl]

  private lazy val explorer = wire[OpeningExplorer]

  lazy val config = wire[OpeningConfigStore]

  lazy val wiki = wire[OpeningWikiApi]

  lazy val api = wire[OpeningApi]

  lazy val search = wire[OpeningSearch]
}

trait ExplorerEndpoint
private trait WikiColl
