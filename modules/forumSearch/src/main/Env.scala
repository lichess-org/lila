package lila.forumSearch

import akka.actor._
import com.typesafe.config.Config

import lila.forum.PostApi
import lila.search._

final class Env(
    config: Config,
    postApi: PostApi,
    makeClient: Index => ESClient,
    system: ActorSystem
) {

  private val IndexName = config getString "index"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private val client = makeClient(Index(IndexName))

  val api = new ForumSearchApi(client, postApi)

  def apply(text: String, page: Int, troll: Boolean) =
    paginatorBuilder(Query(text, troll), page)

  def cli = new lila.common.Cli {
    def process = {
      case "forum" :: "search" :: "reset" :: Nil => api.reset inject "done"
    }
  }

  import Query.jsonWriter

  private lazy val paginatorBuilder = new lila.search.PaginatorBuilder(
    searchApi = api,
    maxPerPage = lila.common.MaxPerPage(PaginatorMaxPerPage)
  )

  system.actorOf(Props(new Actor {
    import lila.forum.actorApi._
    def receive = {
      case InsertPost(post) => api store post
      case RemovePost(id) => client deleteById Id(id)
      case RemovePosts(ids) => client deleteByIds ids.map(Id.apply)
    }
  }), name = ActorName)
}

object Env {

  lazy val current = "forumSearch" boot new Env(
    config = lila.common.PlayApp loadConfig "forumSearch",
    postApi = lila.forum.Env.current.postApi,
    makeClient = lila.search.Env.current.makeClient,
    system = lila.common.PlayApp.system
  )
}
