package lila.forumSearch

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*
import lila.search.*

@Module
private class ForumSearchConfig(
    @ConfigName("index") val indexName: String,
    @ConfigName("paginator.max_per_page") val maxPerPage: MaxPerPage,
    @ConfigName("actor.name") val actorName: String
)

final class Env(
    appConfig: Configuration,
    makeClient: Index => ESClient,
    postApi: lila.forum.ForumPostApi,
    postRepo: lila.forum.ForumPostRepo
)(using
    ec: Executor,
    system: ActorSystem,
    mat: akka.stream.Materializer
):

  private val config = appConfig.get[ForumSearchConfig]("forumSearch")(AutoConfig.loader)

  private lazy val client = makeClient(Index(config.indexName))

  lazy val api: ForumSearchApi = wire[ForumSearchApi]

  def apply(text: String, page: Int, troll: Boolean) =
    paginatorBuilder(Query(text take 100, troll), page)

  def cli =
    new lila.common.Cli:
      def process = { case "forum" :: "search" :: "reset" :: Nil =>
        api.reset inject "done"
      }

  private lazy val paginatorBuilder = lila.search.PaginatorBuilder(api, config.maxPerPage)

  system.actorOf(
    Props(new Actor {
      import lila.forum.*
      def receive = {
        case InsertPost(post) => api.store(post).unit
        case RemovePost(id)   => client.deleteById(id into Id).unit
        case RemovePosts(ids) => client.deleteByIds(Id.from[List, ForumPostId](ids)).unit
      }
    }),
    name = config.actorName
  )
