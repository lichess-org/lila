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
    system: ActorSystem) {

  private val settings = new {
    val EntryMax = config getInt "lobby.entry.max"
    val MessageMax = config getInt "lobby.message.max"
    val MessageTtl = config duration "lobby.message.ttl"
    val CollectionHook = config getString "lobby.collection.hook"
    val CollectionEntry = config getString "lobby.collection.entry"
    val CollectionMessage = config getString "lobby.collection.message"
    val NetDomain = config getString "net.domain"
    val SocketName = config getString "socket.name"
    val SocketUidTtl = config duration "socket.uid.ttl"
    val OrphanHookTtl = config duration "orphan_hook.ttl"
  }
  import settings._

  lazy val socket = system.actorOf(Props(new Socket(
    messenger = messenger,
    history = history,
    uidTtl = SocketUidTtl
  )), name = SocketName)

  lazy val socketHandler = new SocketHandler(socket = socket, flood = flood)

  lazy val fisherman = new Fisherman(hookMemo, socket)

  private lazy val history = new History(ttl = MessageTtl)

  private lazy val messenger = new Messenger(netDomain = NetDomain)

  private lazy val hookMemo = new ExpireSetMemo(ttl = OrphanHookTtl)

  private[lobby] lazy val hookColl = db(CollectionHook)
  private[lobby] lazy val entryColl = db(CollectionEntry)
  private[lobby] lazy val messageColl = db(CollectionMessage)
}

object Env {

  lazy val current = "[boot] lobby" describes new Env(
    config = lila.common.PlayApp loadConfig "lobby",
    db = lila.db.Env.current,
    flood = lila.security.Env.current.flood,
    system = lila.common.PlayApp.system)
}
