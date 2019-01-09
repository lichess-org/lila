package lila.chat

import akka.actor.{ ActorSystem, Props, ActorSelection }
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    flood: lila.security.Flood,
    spam: lila.security.Spam,
    shutup: ActorSelection,
    modLog: ActorSelection,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: ActorSystem
) {

  private val settings = new {
    val CollectionChat = config getString "collection.chat"
    val CollectionTimeout = config getString "collection.timeout"
    val MaxLinesPerChat = config getInt "max_lines"
    val NetDomain = config getString "net.domain"
    val ActorName = config getString "actor.name"
    val TimeoutDuration = config duration "timeout.duration"
    val TimeoutCheckEvery = config duration "timeout.check_every"
  }
  import settings._

  val timeout = new ChatTimeout(
    coll = timeoutColl,
    duration = TimeoutDuration
  )

  val api = new ChatApi(
    coll = chatColl,
    chatTimeout = timeout,
    flood = flood,
    spam = spam,
    shutup = shutup,
    modLog = modLog,
    asyncCache = asyncCache,
    lilaBus = system.lilaBus,
    maxLinesPerChat = MaxLinesPerChat,
    netDomain = NetDomain
  )

  val panic = new ChatPanic

  system.scheduler.schedule(TimeoutCheckEvery, TimeoutCheckEvery) {
    timeout.checkExpired foreach api.userChat.reinstate
  }

  system.actorOf(Props(new FrontActor(api)), name = ActorName)

  private[chat] lazy val chatColl = db(CollectionChat)
  private[chat] lazy val timeoutColl = db(CollectionTimeout)
}

object Env {

  lazy val current: Env = "chat" boot new Env(
    config = lila.common.PlayApp loadConfig "chat",
    db = lila.db.Env.current,
    flood = lila.security.Env.current.flood,
    spam = lila.security.Env.current.spam,
    shutup = lila.hub.Env.current.shutup,
    modLog = lila.hub.Env.current.mod,
    asyncCache = lila.memo.Env.current.asyncCache,
    system = lila.common.PlayApp.system
  )
}
