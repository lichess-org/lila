package lila.appeal

import com.softwaremill.macwire.*

import lila.core.config.*
import lila.common.Bus

@Module
final class Env(db: lila.db.Db, cacheApi: lila.memo.CacheApi)(using Executor)(using scheduler: Scheduler):

  private val coll = db(CollName("appeal2"))

  private lazy val snoozer = lila.memo.Snoozer[Appeal.SnoozeKey]("appeal.snooze", cacheApi)

  lazy val api: AppealApi = wire[AppealApi]

  scheduler.scheduleWithFixedDelay(55.minutes, 1.hour): () =>
    api.countUnread.foreach(lila.mon.mod.queueStatus("appeal", 40).update(_))
    api.reopenPausedAppeals()

  Bus.sub[lila.core.mod.MarkBooster]: m =>
    api.toggleClosed(m.userId, AppealTopic.boost, m.value.not)
  Bus.sub[lila.core.mod.MarkCheater]: m =>
    api.toggleClosed(m.userId, AppealTopic.cheat, m.value.not)
  Bus.sub[lila.core.mod.Shadowban]: m =>
    api.toggleClosed(m.userId, AppealTopic.comm, m.value.not)
  Bus.sub[lila.core.security.CloseAccount]: m =>
    api.toggleClosedAllOf(m.userId, true)
  Bus.sub[lila.core.security.ReopenAccount]: m =>
    api.toggleClosed(m.user.id, AppealTopic.close, false)
  Bus.sub[lila.core.mod.RankBan]: k =>
    api.toggleClosed(k.userId, AppealTopic.rank, k.value.not)
  Bus.sub[lila.core.mod.ArenaBan]: k =>
    api.toggleClosed(k.userId, AppealTopic.arena, k.value.not)
  Bus.sub[lila.core.mod.PrizeBan]: k =>
    api.toggleClosed(k.userId, AppealTopic.prize, k.value.not)
