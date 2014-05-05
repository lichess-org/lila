package lila.timeline

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    getFriendIds: String => Fu[Set[String]],
    getFollowerIds: String => Fu[Set[String]],
    lobbySocket: ActorSelection,
    renderer: ActorSelection,
    system: ActorSystem) {

  private val UserCollectionEntry = config getString "user.collection.entry"
  private val UserDisplayMax = config getInt "user.display_max"
  private val UserActorName = config getString "user.actor.name"

  lazy val getter = new Getter(
    userMax = UserDisplayMax)

  system.actorOf(Props(new Push(
    lobbySocket = lobbySocket,
    renderer = renderer,
    getFriendIds = getFriendIds,
    getFollowerIds = getFollowerIds
  )), name = UserActorName)

  private[timeline] lazy val entryColl = db(UserCollectionEntry)
}

object Env {

  lazy val current = "[boot] timeline" describes new Env(
    config = lila.common.PlayApp loadConfig "timeline",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    getFriendIds = lila.relation.Env.current.api.friends _,
    getFollowerIds = lila.relation.Env.current.api.followers _,
    lobbySocket = lila.hub.Env.current.socket.lobby,
    renderer = lila.hub.Env.current.actor.renderer,
    system = lila.common.PlayApp.system)
}
