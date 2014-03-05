package lila.forumSearch

import akka.actor._
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config

import lila.forum.PostApi

final class Env(
    config: Config,
    postApi: PostApi,
    client: ElasticClient,
    system: ActorSystem) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"

  private val indexer: ActorRef = system.actorOf(Props(new Indexer(
    client = client,
    indexName = IndexName,
    typeName = TypeName,
    postApi = postApi
  )), name = IndexerName)

  def apply(text: String, page: Int, staff: Boolean, troll: Boolean) = {
    val query = Query(s"$IndexName/$TypeName", text, staff, troll)
    paginatorBuilder(query, page)
  }

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    private implicit def timeout = makeTimeout minutes 20
    def process = {
      case "forum" :: "search" :: "reset" :: Nil =>
        (indexer ? lila.search.actorApi.Reset) inject "Forum search index rebuilt"
    }
  }

  private lazy val paginatorBuilder = new lila.search.PaginatorBuilder(
    indexer = indexer,
    maxPerPage = PaginatorMaxPerPage,
    converter = res => postApi viewsFromIds (res.getHits.hits.toList map (_.id))
  )
}

object Env {

  lazy val current = "[boot] forumSearch" describes new Env(
    config = lila.common.PlayApp loadConfig "forumSearch",
    postApi = lila.forum.Env.current.postApi,
    client = lila.search.Env.current.client,
    system = lila.common.PlayApp.system)
}
