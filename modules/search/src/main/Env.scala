package lila.search

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.config.CollName

@Module
final class Env(
    db: lila.db.Db,
    ws: StandaloneWSClient
)(using Executor):

  val elastic = SearchClient(ws, db(CollName("elasticsearch_events")))
