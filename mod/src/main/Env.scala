package lila.mod

import lila.db.Types.Coll
import lila.security.{ Firewall, UserSpy }
import lila.user.EloUpdater

import akka.actor.ActorRef
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    eloUpdater: EloUpdater,
    lobby: ActorRef,
    firewall: Firewall,
    userSpy: String => Fu[UserSpy]) {

  private val CollectionModlog = config getString "collection.modlog"

  private[mod] lazy val modlogColl = db(CollectionModlog)

  val logApi = new ModlogApi

  lazy val api = new ModApi(
    logApi = logApi,
    userSpy = userSpy,
    firewall = firewall,
    eloUpdater = eloUpdater,
    lobby = lobby)
}

object Env {

  lazy val current = "[boot] mod" describes new Env(
    config = lila.common.PlayApp loadConfig "mod",
    db = lila.db.Env.current,
    eloUpdater = lila.user.Env.current.eloUpdater,
    lobby = lila.hub.Env.current.actor.lobby,
    firewall = lila.security.Env.current.firewall,
    userSpy = lila.security.Env.current.userSpy)
}
