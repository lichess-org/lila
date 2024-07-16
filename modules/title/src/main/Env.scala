package lila.title

import com.softwaremill.macwire.*

import lila.core.config.CollName
import lila.core.lilaism.Lilaism.*

@Module
final class Env(cacheApi: lila.memo.CacheApi, db: lila.db.Db, picfitApi: lila.memo.PicfitApi)(using
    Executor,
    lila.core.config.BaseUrl
)(using scheduler: Scheduler):

  private val requestColl = db(CollName("title_request"))

  val api = wire[TitleApi]

  val form = TitleForm

  scheduler.scheduleWithFixedDelay(5 minutes, 113 minutes)(() => api.cleanupOldPics)
