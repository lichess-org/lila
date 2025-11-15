package lila.chat

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.core.config.*
import lila.core.user.LightUserApi

private case class ChatConfig(
    @ConfigName("collection.chat") chatColl: CollName,
    @ConfigName("collection.timeout") timeoutColl: CollName,
    @ConfigName("timeout.duration") timeoutDuration: FiniteDuration,
    @ConfigName("timeout.check_every") timeoutCheckEvery: FiniteDuration
)

@Module
final class Env(
    appConfig: Configuration,
    netDomain: NetDomain,
    userApi: lila.core.user.UserApi,
    lightUserApi: LightUserApi,
    userRepo: lila.core.user.UserRepo,
    db: lila.db.Db,
    flood: lila.core.security.FloodApi,
    spam: lila.core.security.SpamApi,
    shutupApi: lila.core.shutup.ShutupApi,
    cacheApi: lila.memo.CacheApi
)(using Executor)(using scheduler: Scheduler):

  private val config = appConfig.get[ChatConfig]("chat")(using AutoConfig.loader)
  import config.*

  lazy val timeout = ChatTimeout(
    coll = db(timeoutColl),
    duration = timeoutDuration
  )

  lazy val coll = db(chatColl)

  lazy val json = wire[ChatJsonView]

  lazy val api = wire[ChatApi]

  scheduler.scheduleWithFixedDelay(timeoutCheckEvery, timeoutCheckEvery): () =>
    timeout.checkExpired.foreach(api.userChat.reinstate)
