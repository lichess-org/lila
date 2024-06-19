package lila.forumSearch

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.core.forum.BusForum
import BusForum.*
import lila.core.config.ConfigName
import lila.search.client.SearchClient
import lila.search.spec.Query

@Module
private class ForumSearchConfig(@ConfigName("paginator.max_per_page") val maxPerPage: MaxPerPage)

final class Env(
    appConfig: Configuration,
    postApi: lila.core.forum.ForumPostApi,
    client: SearchClient
)(using Executor):

  private val config = appConfig.get[ForumSearchConfig]("forumSearch")(AutoConfig.loader)

  lazy val api: ForumSearchApi = wire[ForumSearchApi]

  def apply(text: String, page: Int, troll: Boolean) =
    paginatorBuilder(Query.forum(text.take(100), troll), page)

  private lazy val paginatorBuilder = lila.search.PaginatorBuilder(api, config.maxPerPage)

  lila.common.Bus.sub[BusForum]:
    case ErasePosts(ids) => client.deleteByIds(index, ids.map(_.value))
