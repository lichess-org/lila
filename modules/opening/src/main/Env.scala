package lila.opening

import com.softwaremill.macwire._
import com.softwaremill.tagging._
import scala.concurrent.duration._

import lila.common.config
import lila.memo.CacheApi
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

@Module
final class Env(
    cacheApi: CacheApi,
    appConfig: Configuration,
    ws: StandaloneWSClient
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mode: play.api.Mode
) {

  private val explorerEndpoint = appConfig.get[String]("explorer.endpoint").taggedWith[ExplorerEndpoint]

  lazy val api = wire[OpeningApi]
}

trait ExplorerEndpoint
