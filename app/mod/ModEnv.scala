package lila
package mod

import user.{ User, UserRepo }
import elo.EloUpdater
import lobby.Messenger
import core.Settings
import security.{ Firewall, Store => SecurityStore }

import com.mongodb.casbah.MongoCollection

final class ModEnv(
    settings: Settings,
    userRepo: UserRepo,
    securityStore: SecurityStore,
    firewall: Firewall,
    eloUpdater: EloUpdater,
    lobbyMessenger: Messenger,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val modlogRepo = new ModlogRepo(mongodb(ModlogCollectionModlog))

  lazy val logApi = new ModlogApi(
    repo = modlogRepo)

  lazy val api = new ModApi(
    logApi = logApi,
    userRepo = userRepo,
    securityStore = securityStore,
    firewall = firewall,
    eloUpdater = eloUpdater,
    lobbyMessenger = lobbyMessenger)
}
