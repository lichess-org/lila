package lidraughts.forumSearch

import akka.actor._
import com.typesafe.config.Config

import lidraughts.forum.PostApi
import lidraughts.search._

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

  def apply(text: String, page: Int, staff: Boolean, troll: Boolean) =
    paginatorBuilder(Query(text, staff, troll), page)

  def cli = new lidraughts.common.Cli {
    def process = {
      case "forum" :: "search" :: "reset" :: Nil => api.reset inject "done"
    }
  }

  import Query.jsonWriter

  private lazy val paginatorBuilder = new lidraughts.search.PaginatorBuilder(
    searchApi = api,
    maxPerPage = lidraughts.common.MaxPerPage(PaginatorMaxPerPage)
  )

  system.actorOf(Props(new Actor {
    import lidraughts.forum.actorApi._
    def receive = {
      case InsertPost(post) => api store post
      case RemovePost(id) => client deleteById Id(id)
      case RemovePosts(ids) => client deleteByIds ids.map(Id.apply)
    }
  }), name = ActorName)
}

object Env {

  lazy val current = "forumSearch" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "forumSearch",
    postApi = lidraughts.forum.Env.current.postApi,
    makeClient = lidraughts.search.Env.current.makeClient,
    system = lidraughts.common.PlayApp.system
  )
}
