package lila.playban

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config.CollName

@Module
final class Env(
    appConfig: Configuration,
    messenger: lila.message.MessageApi,
    chatApi: lila.chat.ChatApi,
    userRepo: lila.user.UserRepo,
    lightUser: lila.common.LightUser.Getter,
    db: lila.db.Db,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private lazy val playbanColl = db(
    CollName(appConfig.get[String]("playban.collection.playban"))
  )

  private lazy val feedback = wire[PlaybanFeedback]

  private lazy val sandbag = wire[SandbagWatch]

  lazy val api = wire[PlaybanApi]
}
