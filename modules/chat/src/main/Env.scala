package lila.chat

import akka.actor.{ ActorSystem, Props, ActorSelection }
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    flood: lila.security.Flood,
    shutup: ActorSelection,
    modLog: ActorSelection,
    system: ActorSystem) {

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
    duration = TimeoutDuration)

  val api = new ChatApi(
    coll = chatColl,
    chatTimeout = timeout,
    flood = flood,
    shutup = shutup,
    modLog = modLog,
    lilaBus = system.lilaBus,
    maxLinesPerChat = MaxLinesPerChat,
    netDomain = NetDomain)

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
    shutup = lila.hub.Env.current.actor.shutup,
    modLog = lila.hub.Env.current.actor.mod,
    system = lila.common.PlayApp.system)
}
