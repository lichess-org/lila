package lila.lobby

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.memo.ExpireSetMemo
import lila.socket.History

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    blocks: (String, String) => Fu[Boolean],
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val MessageTtl = config duration "message.ttl"
    val NetDomain = config getString "net.domain"
    val SocketName = config getString "socket.name"
    val SocketUidTtl = config duration "socket.uid.ttl"
    val OrphanHookTtl = config duration "orphan_hook.ttl"
    val ActorName = config getString "actor.name"
  }
  import settings._

  private val socket = system.actorOf(Props(new Socket(
    history = history,
    router = hub.actor.router,
    uidTtl = SocketUidTtl
  )), name = SocketName)

  val lobby = system.actorOf(Props(new Lobby(
    biter = biter,
    socket = socket
  )), name = ActorName)

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    lobby = lobby,
    socket = socket)

  lazy val history = new History(ttl = MessageTtl)

  {
    import scala.concurrent.duration._

    scheduler.once(5 seconds) {
      scheduler.message(1 seconds) {
        lobby -> lila.socket.actorApi.Broom
      }
      scheduler.message(10 seconds) {
        lobby -> actorApi.Resync
      }
    }
  }

  private lazy val biter = new Biter(blocks)
}

object Env {

  lazy val current = "[boot] lobby" describes new Env(
    config = lila.common.PlayApp loadConfig "lobby",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    blocks = lila.relation.Env.current.api.blocks,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
