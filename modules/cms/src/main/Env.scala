package lila.cms

import com.softwaremill.macwire.*

import lila.core.config.CollName
import lila.core.id.CmsPageKey
import lila.memo.CacheApi
import lila.cms.CmsPage.Render

@Module
final class Env(
    db: lila.db.Db,
    cacheApi: CacheApi,
    langList: lila.core.i18n.LangList,
    langPicker: lila.core.i18n.LangPicker
)(using Executor):

  private val coll = db(CollName("cms_page"))

  private val markup = wire[CmsMarkup]

  lazy val api = wire[CmsApi]

  export api.render
  def renderKey(key: String)(using lila.ui.Context): Future[Option[Render]] = api.render(CmsPageKey(key))

  val form = wire[CmsForm]
