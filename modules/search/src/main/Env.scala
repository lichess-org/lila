package lila.search

import com.softwaremill.macwire.*
import lila.common.autoconfig.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

@Module
private class SearchConfig(
    val enabled: Boolean,
    val writeable: Boolean,
    val endpoint: String
)

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient
)(using Executor):

  private val config = appConfig.get[SearchConfig]("search")(AutoConfig.loader)

  private def makeHttp(index: Index): ESClientHttp = wire[ESClientHttp]

  val makeClient = (index: Index) =>
    if config.enabled then makeHttp(index)
    else wire[ESClientStub]
