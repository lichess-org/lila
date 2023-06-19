package lila.chat

import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*

private case class ChatConfig(
    @ConfigName("collection.chat") chatColl: CollName,
    @ConfigName("collection.timeout") timeoutColl: CollName,
    @ConfigName("actor.name") actorName: String,
    @ConfigName("timeout.duration") timeoutDuration: FiniteDuration,
    @ConfigName("timeout.check_every") timeoutCheckEvery: FiniteDuration
)

@Module
final class Env(
    appConfig: Configuration,
    netDomain: NetDomain,
    userRepo: lila.user.UserRepo,
    db: lila.db.Db,
    flood: lila.security.Flood,
    spam: lila.security.Spam,
    shutup: lila.hub.actors.Shutup,
    cacheApi: lila.memo.CacheApi
)(using
    ec: Executor,
    scheduler: Scheduler
):

  private val config = appConfig.get[ChatConfig]("chat")(AutoConfig.loader)
  import config.*

  lazy val timeout = new ChatTimeout(
    coll = db(timeoutColl),
    duration = timeoutDuration
  )

  lazy val coll = db(chatColl)

  lazy val api = wire[ChatApi]

  lazy val panic = wire[ChatPanic]

  scheduler.scheduleWithFixedDelay(timeoutCheckEvery, timeoutCheckEvery): () =>
    timeout.checkExpired foreach api.userChat.reinstate
