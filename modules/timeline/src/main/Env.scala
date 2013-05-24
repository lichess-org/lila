package lila.timeline

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    getUsername: String ⇒ Fu[String],
    lobbySocket: lila.hub.ActorLazyRef,
    renderer: lila.hub.ActorLazyRef,
    system: ActorSystem) {

  private val GameCollectionEntry = config getString "game.collection.entry"
  private val GameDisplayMax = config getInt "game.display_max"
  private val GameActorName = config getString "game.actor.name"
  private val UserCollectionEntry = config getString "user.collection.entry"
  private val UserDisplayMax = config getInt "user.display_max"
  private val UserActorName = config getString "user.actor.name"

  lazy val getter = new Getter(
    gameMax = GameDisplayMax,
    userMax = UserDisplayMax)

  system.actorOf(Props(new GamePush(
    lobbySocket = lobbySocket,
    renderer = renderer,
    getUsername = getUsername
  )), name = GameActorName)

  system.actorOf(Props(new Push(
    lobbySocket = lobbySocket,
    renderer = renderer
  )), name = UserActorName)

  private[timeline] lazy val gameEntryColl = db(GameCollectionEntry)

  private[timeline] lazy val entryColl = db(UserCollectionEntry)
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
