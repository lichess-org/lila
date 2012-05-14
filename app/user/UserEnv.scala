package lila
package user

import com.mongodb.casbah.MongoCollection

import core.Settings

final class UserEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val historyRepo = new HistoryRepo(mongodb(MongoCollectionHistory))

  lazy val userRepo = new UserRepo(mongodb(MongoCollectionUser))

  lazy val eloUpdater = new EloUpdater(
    userRepo = userRepo,
    historyRepo = historyRepo)

  lazy val usernameMemo = new UsernameMemo(timeout = MemoUsernameTimeout)
}
