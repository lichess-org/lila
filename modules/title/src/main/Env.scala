package lila.title

import com.softwaremill.macwire.*

import lila.core.config.CollName
import lila.core.lilaism.Lilaism.*
import lila.core.user.PublicFideIdOf

@Module
final class Env(
    db: lila.db.Db,
    picfitApi: lila.memo.PicfitApi,
    cacheApi: lila.memo.CacheApi,
    baseUrl: lila.core.config.BaseUrl,
    userApi: lila.core.user.UserApi
)(using
    ec: Executor,
    scheduler: Scheduler
):

  private val requestColl = db(CollName("title_request"))

  val api = wire[TitleApi]

  val fideIdOf: PublicFideIdOf = api.publicFideIdOf.apply

  val form = TitleForm

  scheduler.scheduleWithFixedDelay(25.minutes, 113.minutes)(() => api.cleanupOldPics)
