package lila.lobby

import lila.common.PimpedConfig._
import lila.socket.History
import lila.memo.ExpireSetMemo

import com.typesafe.config.Config
import akka.actor._

final class Env(
    config: Config,
    db: lila.db.Env,
    flood: lila.security.Flood,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val MessageMax = config getInt "message.max"
    val MessageTtl = config duration "message.ttl"
    val CollectionHook = config getString "collection.hook"
    val CollectionMessage = config getString "collection.message"
    val NetDomain = config getString "net.domain"
    val SocketName = config getString "socket.name"
    val SocketUidTtl = config duration "socket.uid.ttl"
    val OrphanHookTtl = config duration "orphan_hook.ttl"
  }
  import settings._

  private val socket = system.actorOf(Props(new Socket(
    messenger = messenger,
    history = history,
    uidTtl = SocketUidTtl
  )), name = SocketName)

  lazy val socketHandler = new SocketHandler(socket = socket, flood = flood)

  lazy val fisherman = new Fisherman(hookMemo, socket)

  {
    import scala.concurrent.duration._

    scheduler.message(1 second) {
      socket -> actorApi.WithHooks(hookMemo.putAll)
    }

    scheduler.future(2 seconds, "fisherman: cleanup") {
      fisherman.cleanup
    }

    scheduler.future(20 seconds, "lobby: cleanup") {
      HookRepo.cleanupOld
    }
  }

  lazy val history = new History(ttl = MessageTtl)

  private lazy val messenger = new Messenger(NetDomain)

  private lazy val hookMemo = new ExpireSetMemo(ttl = OrphanHookTtl)

  private[lobby] lazy val hookColl = db(CollectionHook)
  private[lobby] lazy val messageColl = db(CollectionMessage)
}

object Env {

  lazy val current = "[boot] lobby" describes new Env(
    config = lila.common.PlayApp loadConfig "lobby",
    db = lila.db.Env.current,
    flood = lila.security.Env.current.flood,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
