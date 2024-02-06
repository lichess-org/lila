package lila.cms

import com.softwaremill.macwire.*

import lila.db.dsl.Coll
import lila.memo.CacheApi
import lila.common.config.CollName

@Module
final class Env(db: lila.db.Db, cacheApi: CacheApi)(using Executor, Scheduler):

  private val coll = db(CollName("cms_page"))

  lazy val api = wire[CmsApi]

  def form = CmsForm
