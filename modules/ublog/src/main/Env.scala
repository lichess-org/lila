package lila.ublog

import com.github.blemale.scaffeine.AsyncLoadingCache
import com.softwaremill.macwire.*
import play.api.{ ConfigLoader, Configuration }
import lila.core.config.*
import lila.db.dsl.Coll
import lila.common.autoconfig.{ *, given }
import lila.common.Bus

@Module
final private class UblogConfig(
    val searchPageSize: MaxPerPage,
    val carouselSize: Int
)

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.core.user.UserRepo,
    userApi: lila.core.user.UserApi,
    picfitApi: lila.memo.PicfitApi,
    ircApi: lila.core.irc.IrcApi,
    relationApi: lila.core.relation.RelationApi,
    shutupApi: lila.core.shutup.ShutupApi,
    captcha: lila.core.captcha.CaptchaApi,
    cacheApi: lila.memo.CacheApi,
    langList: lila.core.i18n.LangList,
    net: NetConfig,
    appConfig: Configuration,
    settingStore: lila.memo.SettingStore.Builder,
    client: lila.search.client.SearchClient,
    reportApi: lila.report.ReportApi
)(using Executor, Scheduler, play.api.Mode):

  export net.{ assetBaseUrl, baseUrl, domain, assetDomain }

  private val config = appConfig.get[UblogConfig]("ublog")(using AutoConfig.loader)
  private val colls = UblogColls(db(CollName("ublog_blog")), db(CollName("ublog_post")))

  val topic = wire[UblogTopicApi]

  val automod = wire[UblogAutomod]

  val api: UblogApi = wire[UblogApi]

  val search: UblogSearch = wire[UblogSearch]

  val paginator = wire[UblogPaginator]

  val markup = wire[UblogMarkup]

  val form = wire[UblogForm]

  val viewCounter = wire[UblogViewCounter]

  val lastPostsCache: AsyncLoadingCache[Unit, List[UblogPost.PreviewPost]] =
    cacheApi.unit[List[UblogPost.PreviewPost]]:
      _.refreshAfterWrite(10.seconds).buildAsyncFuture: _ =>
        api.fetchCarouselFromDb().map(_.shuffled)

  Bus.sub[lila.core.mod.Shadowban]:
    case lila.core.mod.Shadowban(userId, v) =>
      api.setShadowban(userId, v)

  import lila.core.security.ReopenAccount
  Bus.sub[ReopenAccount]:
    case ReopenAccount(user) => api.onAccountReopen(user)

final private class UblogColls(val blog: Coll, val post: Coll)
