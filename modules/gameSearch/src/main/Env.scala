package lila.gameSearch

import akka.actor._
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config

import lila.db.api.$find
import lila.game.tube.gameTube

final class Env(
    config: Config,
    system: ActorSystem,
    client: ElasticClient) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"

  private val indexer: ActorRef = system.actorOf(Props(new Indexer(
    client = client,
    indexName = IndexName,
    typeName = TypeName
  )), name = IndexerName)

  lazy val paginator = new lila.search.PaginatorBuilder(
    indexer = indexer,
    maxPerPage = PaginatorMaxPerPage,
    converter = res => $find.byOrderedIds[lila.game.Game](res.getHits.hits.toList map (_.id)))

  lazy val forms = new DataForm

  def nonEmptyQuery(data: SearchData) = data nonEmptyQuery s"$IndexName/$TypeName"

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    private implicit def timeout = makeTimeout minutes 60
    def process = {
      case "game" :: "search" :: "reset" :: Nil =>
        (indexer ? lila.search.actorApi.Reset) inject "Game search index rebuilt"
    }
  }
}

object Env {

  lazy val current = "[boot] gameSearch" describes new Env(
    config = lila.common.PlayApp loadConfig "gameSearch",
    system = lila.common.PlayApp.system,
    client = lila.search.Env.current.client)
}
