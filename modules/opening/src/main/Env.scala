package lila.opening

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.memo.CacheApi
import lila.core.config.{ CollName, Secret }
import lila.common.config.given

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.core.game.GameRepo,
    pgnDump: lila.core.game.PgnDump,
    cacheApi: CacheApi,
    appConfig: Configuration,
    cookieBaker: lila.core.security.LilaCookie,
    ws: StandaloneWSClient
)(using Scheduler, Executor):

  private val explorerEndpoint = Url(appConfig.get[String]("explorer.endpoint"))
  private val oauthToken = appConfig.get[Secret]("explorer.oauth_token")

  private lazy val wikiColl = db(CollName("opening_wiki"))

  private lazy val explorer = wire[OpeningExplorer]

  lazy val config = wire[OpeningConfigStore]

  lazy val wiki = wire[OpeningWikiApi]

  lazy val api = wire[OpeningApi]

  lazy val search = wire[OpeningSearch]
