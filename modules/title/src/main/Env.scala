package lila.title

import com.softwaremill.macwire.*

import lila.core.lilaism.Lilaism.*
import lila.core.config.CollName

@Module
final class Env(cacheApi: lila.memo.CacheApi, db: lila.db.Db, flairApi: lila.core.user.FlairApi)(using
    Executor
):

  private val requestColl = db(CollName("title_request"))

  lazy val api  = wire[TitleApi]
  lazy val form = wire[TitleForm]
