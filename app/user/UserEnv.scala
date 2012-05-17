package lila
package user

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports.ObjectId
import com.mongodb.DBRef

import core.Settings

final class UserEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    dbRef: String ⇒ ObjectId ⇒ DBRef) {

  import settings._

  lazy val historyRepo = new HistoryRepo(mongodb(MongoCollectionHistory))

  lazy val userRepo = new UserRepo(
    collection = mongodb(MongoCollectionUser),
    dbRef = user ⇒ dbRef(MongoCollectionUser)(user.id))

  lazy val eloUpdater = new EloUpdater(
    userRepo = userRepo,
    historyRepo = historyRepo)

  lazy val usernameMemo = new UsernameMemo(timeout = MemoUsernameTimeout)

  lazy val cached = new Cached(userRepo)
}
