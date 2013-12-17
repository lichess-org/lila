package lila.mod

import com.typesafe.config.Config

import lila.db.Types.Coll
import lila.security.{ Firewall, UserSpy }

final class Env(
    config: Config,
    db: lila.db.Env,
    lobbySocket: akka.actor.ActorSelection,
    firewall: Firewall,
    userSpy: String => Fu[UserSpy]) {

  private val CollectionModlog = config getString "collection.modlog"

  private[mod] lazy val modlogColl = db(CollectionModlog)

  lazy val logApi = new ModlogApi

  lazy val api = new ModApi(
    logApi = logApi,
    userSpy = userSpy,
    firewall = firewall,
    lobbySocket = lobbySocket)
}

object Env {

  lazy val current = "[boot] mod" describes new Env(
    config = lila.common.PlayApp loadConfig "mod",
    db = lila.db.Env.current,
    lobbySocket = lila.hub.Env.current.socket.lobby,
    firewall = lila.security.Env.current.firewall,
    userSpy = lila.security.Env.current.userSpy)
}
