package lila.opening

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.core.config.CollName
import lila.memo.CacheApi

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.core.game.GameRepo,
    pgnDump: lila.core.game.PgnDump,
    cacheApi: CacheApi,
    appConfig: Configuration,
    cookieBaker: lila.core.security.LilaCookie,
    ws: StandaloneWSClient
)(using Executor):

  private val explorerEndpoint = appConfig.get[String]("explorer.endpoint").taggedWith[ExplorerEndpoint]
  private lazy val wikiColl = db(CollName("opening_wiki"))

  private lazy val explorer = wire[OpeningExplorer]

  lazy val config = wire[OpeningConfigStore]

  lazy val wiki = wire[OpeningWikiApi]

  lazy val api = wire[OpeningApi]

  lazy val search = wire[OpeningSearch]

trait ExplorerEndpoint
