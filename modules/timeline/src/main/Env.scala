package lila.timeline

import tube.entryTube
import lila.db.api._
import lila.db.Implicits._

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    getUsername: String ⇒ Fu[String],
    lobbySocket: lila.hub.ActorLazyRef,
    renderer: lila.hub.ActorLazyRef,
    system: ActorSystem) {

  private val CollectionEntry = config getString "collection.entry"
  private val DisplayMax = config getInt "display_max"
  private val ActorName = config getString "actor.name"

  def recent: Fu[List[Entry]] = 
    $query($select.all) sort $sort.naturalOrder toListFlatten DisplayMax.some

  system.actorOf(Props(new Push(
    lobbySocket = lobbySocket,
    renderer = renderer,
    getUsername = getUsername
  )), name = ActorName)

  private[timeline] lazy val entryColl = db(CollectionEntry)
}

object Env {

  lazy val current = "[boot] timeline" describes new Env(
    config = lila.common.PlayApp loadConfig "timeline",
    db = lila.db.Env.current,
    getUsername = lila.user.Env.current.usernameOrAnonymous _,
    lobbySocket = lila.hub.Env.current.socket.lobby,
    renderer = lila.hub.Env.current.actor.renderer,
    system = lila.common.PlayApp.system)
}
