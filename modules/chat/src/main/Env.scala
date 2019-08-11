package lidraughts.chat

import akka.actor.{ ActorSystem, Props, ActorSelection }
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    flood: lidraughts.security.Flood,
    spam: lidraughts.security.Spam,
    shutup: ActorSelection,
    modLog: ActorSelection,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
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
    lidraughtsBus = system.lidraughtsBus,
    maxLinesPerChat = MaxLinesPerChat,
    netDomain = NetDomain
  )

  val panic = new ChatPanic

  private val palantir = new Palantir(system.lidraughtsBus)

  system.scheduler.schedule(TimeoutCheckEvery, TimeoutCheckEvery) {
    timeout.checkExpired foreach api.userChat.reinstate
  }

  system.actorOf(Props(new FrontActor(api, palantir)), name = ActorName)

  private[chat] lazy val chatColl = db(CollectionChat)
  private[chat] lazy val timeoutColl = db(CollectionTimeout)
}

object Env {

  lazy val current: Env = "chat" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "chat",
    db = lidraughts.db.Env.current,
    flood = lidraughts.security.Env.current.flood,
    spam = lidraughts.security.Env.current.spam,
    shutup = lidraughts.hub.Env.current.shutup,
    modLog = lidraughts.hub.Env.current.mod,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    system = lidraughts.common.PlayApp.system
  )
}
