package lila.title

import com.softwaremill.macwire.*

import lila.core.lilaism.Lilaism.*
import lila.core.config.CollName

@Module
final class Env(cacheApi: lila.memo.CacheApi, db: lila.db.Db, picfitApi: lila.memo.PicfitApi)(using
    Executor,
    lila.core.config.BaseUrl
):

  private val requestColl = db(CollName("title_request"))

  val api = wire[TitleApi]

  val form = TitleForm
