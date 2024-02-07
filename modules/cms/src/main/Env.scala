package lila.cms

import com.softwaremill.macwire.*

import lila.db.dsl.Coll
import lila.memo.CacheApi
import lila.common.config.{ CollName, BaseUrl, AssetBaseUrl }

@Module
final class Env(db: lila.db.Db, cacheApi: CacheApi, baseUrl: BaseUrl, assetBaseUrl: AssetBaseUrl)(using
    Executor,
    Scheduler,
    play.api.Mode
):

  private val coll = db(CollName("cms_page"))

  private val markup = wire[CmsMarkup]

  lazy val api = wire[CmsApi]

  def form = CmsForm
