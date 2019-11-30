package lila.chat

import akka.actor.{ ActorSystem, Props, ActorSelection }
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration.FiniteDuration

import lila.common.config._

private case class ChatConfig(
    @ConfigName("collection.chat") chatColl: CollName,
    @ConfigName("collection.timeout") timeoutColl: CollName,
    @ConfigName("max_lines") maxLines: Chat.MaxLines,
    @ConfigName("actor.name") actorName: String,
    net: NetConfig,
    @ConfigName("timeout.duration") timeoutDuration: FiniteDuration,
    @ConfigName("timeout.check_every") timeoutCheckEvery: FiniteDuration
)

final class Env(
    appConfig: Configuration,
    userRepo: lila.user.UserRepo,
    db: lila.db.Env,
    flood: lila.security.Flood,
    spam: lila.security.Spam,
    shutup: ActorSelection,
    modLog: ActorSelection,
    asyncCache: lila.memo.AsyncCache.Builder
)(implicit system: ActorSystem) {

  private implicit val maxPerLineLoader = intLoader(Chat.MaxLines.apply)
  private val config = appConfig.get[ChatConfig]("chat")(AutoConfig.loader)
  import config._

  val timeout = new ChatTimeout(
    coll = db(timeoutColl),
    duration = timeoutDuration
  )

  val api = new ChatApi(
    coll = db(chatColl),
    userRepo = userRepo,
    chatTimeout = timeout,
    flood = flood,
    spam = spam,
    shutup = shutup,
    modLog = modLog,
    asyncCache = asyncCache,
    maxLinesPerChat = maxLines,
    net = net
  )

  val panic = wire[ChatPanic]

  system.scheduler.scheduleWithFixedDelay(timeoutCheckEvery, timeoutCheckEvery) {
    () => timeout.checkExpired foreach api.userChat.reinstate
  }
}
