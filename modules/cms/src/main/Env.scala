package lila.cms

import com.softwaremill.macwire.*

import lila.core.config.{ AssetBaseUrl, BaseUrl, CollName }
import lila.core.id.CmsPageKey
import lila.memo.CacheApi

@Module
final class Env(
    db: lila.db.Db,
    cacheApi: CacheApi,
    baseUrl: BaseUrl,
    assetBaseUrl: AssetBaseUrl,
    langList: lila.core.i18n.LangList,
    langPicker: lila.core.i18n.LangPicker
)(using Executor, Scheduler, play.api.Mode):

  private val coll = db(CollName("cms_page"))

  private val markup = wire[CmsMarkup]

  lazy val api = wire[CmsApi]

  export api.render
  def renderKey(key: String)(using lila.ui.Context) = api.render(CmsPageKey(key))

  val form = wire[CmsForm]
