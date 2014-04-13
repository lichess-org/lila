package lila.mod

import akka.actor._
import com.typesafe.config.Config

import lila.db.Types.Coll
import lila.security.{ Firewall, UserSpy }

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    lobbySocket: akka.actor.ActorSelection,
    firewall: Firewall,
    userSpy: String => Fu[UserSpy]) {

  private val CollectionModlog = config getString "collection.modlog"
  private val ActorName = config getString "actor.name"

  private[mod] lazy val modlogColl = db(CollectionModlog)

  lazy val logApi = new ModlogApi

  lazy val api = new ModApi(
    logApi = logApi,
    userSpy = userSpy,
    firewall = firewall,
    lilaBus = system.lilaBus,
    lobbySocket = lobbySocket)

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.mod.MarkCheater(userId) => api autoAdjust userId
    }
  }), name = ActorName)
}

object Env {

  lazy val current = "[boot] mod" describes new Env(
    config = lila.common.PlayApp loadConfig "mod",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    lobbySocket = lila.hub.Env.current.socket.lobby,
    firewall = lila.security.Env.current.firewall,
    userSpy = lila.security.Env.current.userSpy)
}
