package lila.playban

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.core.config.CollName

@Module
final class Env(
    appConfig: Configuration,
    messenger: lila.core.msg.MsgApi,
    reportApi: lila.core.report.ReportApi,
    chatApi: lila.core.chat.ChatApi,
    gameApi: lila.core.game.GameApi,
    noteApi: lila.core.user.NoteApi,
    userApi: lila.core.user.UserApi,
    lightUser: lila.core.LightUser.Getter,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi
)(using Executor, play.api.Mode):

  private val playbanColl = db(CollName(appConfig.get[String]("playban.collection.playban")))

  private val feedback = wire[PlaybanFeedback]

  val api = wire[PlaybanApi]
  export api.{ bansOf, HasCurrentPlayban, rageSitOf }
