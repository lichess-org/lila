package lila.search

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import play.api.Configuration

import lila.core.config.CollName

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    ws: StandaloneWSClient
)(using Executor):

  private val endpoint = Url(appConfig.get[String]("search.elastic.endpoint"))
  val elastic = SearchClient(ws, endpoint, db(CollName("elasticsearch_events")))
