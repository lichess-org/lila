package lila.playban

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.config.CollName

@Module
final class Env(
    appConfig: Configuration,
    messenger: lila.msg.MsgApi,
    reporter: lila.hub.actors.Report,
    chatApi: lila.chat.ChatApi,
    userRepo: lila.user.UserRepo,
    noteApi: lila.user.NoteApi,
    lightUser: lila.common.LightUser.Getter,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi
)(using ec: Executor, mode: play.api.Mode):

  private lazy val playbanColl = db(
    CollName(appConfig.get[String]("playban.collection.playban"))
  )

  private lazy val feedback = wire[PlaybanFeedback]

  lazy val api = wire[PlaybanApi]
