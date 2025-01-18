package lila.playban

import com.softwaremill.macwire.*
import lila.core.config.CollName

@Module
final class Env(
    messenger: lila.core.msg.MsgApi,
    chatApi: lila.core.chat.ChatApi,
    gameApi: lila.core.game.GameApi,
    noteApi: lila.core.user.NoteApi,
    userApi: lila.core.user.UserApi,
    userTrustApi: lila.core.security.UserTrustApi,
    lightUser: lila.core.LightUser.Getter,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi
)(using Executor, play.api.Mode):

  private val playbanColl = db(CollName("playban"))

  private val feedback = wire[PlaybanFeedback]

  val api = wire[PlaybanApi]
  export api.{ bansOf, hasCurrentPlayban, rageSitOf }
