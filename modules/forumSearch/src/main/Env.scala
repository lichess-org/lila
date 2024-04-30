package lila.forumSearch

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.search.*
import lila.core.forum.BusForum
import BusForum.*
import lila.core.config.ConfigName
import lila.core.id.ForumPostId

@Module
private class ForumSearchConfig(
    @ConfigName("index") val indexName: String,
    @ConfigName("paginator.max_per_page") val maxPerPage: MaxPerPage
)

final class Env(
    appConfig: Configuration,
    makeClient: Index => ESClient,
    postApi: lila.core.forum.ForumPostApi
)(using Executor, akka.stream.Materializer):

  private val config = appConfig.get[ForumSearchConfig]("forumSearch")(AutoConfig.loader)

  private lazy val client = makeClient(Index(config.indexName))

  lazy val api: ForumSearchApi = wire[ForumSearchApi]

  def apply(text: String, page: Int, troll: Boolean) =
    paginatorBuilder(Query(text.take(100), troll), page)

  def cli: lila.common.Cli = new:
    def process = { case "forum" :: "search" :: "reset" :: Nil =>
      api.reset.inject("done")
    }

  private lazy val paginatorBuilder = lila.search.PaginatorBuilder(api, config.maxPerPage)

  lila.common.Bus.sub[BusForum]:
    case CreatePost(post)        => api.store(post)
    case RemovePost(id, _, _, _) => client.deleteById(id.into(Id))
    case RemovePosts(ids)        => client.deleteByIds(Id.from[List, ForumPostId](ids))
    case ErasePost(id)           => client.deleteById(id.into(Id))
    case ErasePosts(ids)         => client.deleteByIds(Id.from[List, ForumPostId](ids))
