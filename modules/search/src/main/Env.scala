package lila.search

import org.apache.http.HttpHost
import org.apache.pekko.actor.CoordinatedShutdown
import org.elasticsearch.client.RestClient

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.Lilakka
import lila.core.config.CollName

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    shutdown: CoordinatedShutdown
)(using Executor):

  private val restClient =
    RestClient.builder(HttpHost.create(appConfig.get[String]("search.elastic.endpoint"))).build()

  val elastic = SearchClient(
    ElasticsearchAsyncClient(RestClientTransport(restClient, JacksonJsonpMapper())),
    db(CollName("elasticsearch_events")),
    cacheApi
  )

  Lilakka.shutdown(shutdown, _.PhaseServiceStop, "closing elasticsearch client"): () =>
    fuccess(restClient.close())
