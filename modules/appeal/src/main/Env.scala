package lila.appeal

import com.softwaremill.macwire.*

import lila.common.config.*

@Module
final class Env(
    db: lila.db.Db,
    noteApi: lila.user.NoteApi,
    userRepo: lila.user.UserRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  private val coll = db(CollName("appeal"))

  private lazy val snoozer = new lila.memo.Snoozer[Appeal.SnoozeKey](cacheApi)

  lazy val api: AppealApi = wire[AppealApi]
