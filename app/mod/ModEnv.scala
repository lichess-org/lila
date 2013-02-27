package lila
package mod

import user.{ User, UserRepo }
import elo.EloUpdater
import lobby.Messenger
import core.Settings
import security.{ Firewall, UserSpy }

import com.mongodb.casbah.MongoCollection
import scalaz.effects.IO

final class ModEnv(
    settings: Settings,
    userRepo: UserRepo,
    userSpy: String ⇒ IO[UserSpy],
    firewall: Firewall,
    eloUpdater: EloUpdater,
    lobbyMessenger: Messenger,
    mongodb: String ⇒ MongoCollection,
    db: LilaDB) {

  import settings._

  lazy val modlogRepo = new ModlogRepo(db, ModlogCollectionModlog)

  lazy val logApi = new ModlogApi(repo = modlogRepo)

  lazy val api = new ModApi(
    logApi = logApi,
    userRepo = userRepo,
    userSpy = userSpy,
    firewall = firewall,
    eloUpdater = eloUpdater,
    lobbyMessenger = lobbyMessenger)
}
