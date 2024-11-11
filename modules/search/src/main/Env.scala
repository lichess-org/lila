package lila.search

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.autoconfig.*
import lila.search.client.SearchClient

@Module
private class SearchConfig(val enabled: Boolean, val endpoint: String)

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient
)(using Executor):

  private val config = appConfig.get[SearchConfig]("search")(AutoConfig.loader)

  val client: SearchClient =
    val _client =
      if config.enabled then SearchClient.play(ws, s"${config.endpoint}/api") else SearchClient.noop
    LilaSearchClient(_client)
