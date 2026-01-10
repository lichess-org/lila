package lila.ublog

import com.softwaremill.macwire.*
import play.api.{ ConfigLoader, Configuration }
import lila.core.config.*
import lila.db.dsl.Coll
import lila.common.autoconfig.{ *, given }
import lila.common.Bus
import lila.report.Automod

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
    picfitUrl: lila.memo.PicfitUrl,
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
    lightUser: lila.core.LightUser.GetterSync,
    automod: lila.report.Automod
)(using Executor, Scheduler, play.api.Mode):

  export net.{ assetBaseUrl, baseUrl, domain, assetDomain }

  private val config = appConfig.get[UblogConfig]("ublog")(using AutoConfig.loader)
  private val colls = UblogColls(db(CollName("ublog_blog")), db(CollName("ublog_post")))

  val topic = wire[UblogTopicApi]

  val ublogAutomod = wire[UblogAutomod]

  val api: UblogApi = wire[UblogApi]

  val search: UblogSearch = wire[UblogSearch]

  val paginator = wire[UblogPaginator]

  val form = wire[UblogForm]

  val viewCounter = wire[UblogViewCounter]

  val jsonView = wire[UblogJsonView]

  Bus.sub[lila.core.mod.Shadowban]: s =>
    api.setShadowban(s.user, s.value)

  Bus.sub[lila.core.security.ReopenAccount]: r =>
    api.resetDefaultTier(r.user)

  Bus.sub[lila.core.user.SetKidMode]: k =>
    api.resetDefaultTier(k.user)

final private class UblogColls(val blog: Coll, val post: Coll)
