package lila.appeal

import com.softwaremill.macwire.*

import lila.core.config.*

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.core.user.UserRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor)(using scheduler: Scheduler):

  private val coll = db(CollName("appeal"))

  private lazy val snoozer = lila.memo.Snoozer[Appeal.SnoozeKey]("appeal.snooze", cacheApi)

  lazy val api: AppealApi = wire[AppealApi]

  scheduler.scheduleWithFixedDelay(55.minutes, 1.hour): () =>
    api.countUnread.foreach(lila.mon.mod.queueStatus("appeal", 40).update(_))
