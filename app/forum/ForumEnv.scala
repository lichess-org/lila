package lila
package forum

import user.UserRepo
import core.Settings

import com.mongodb.casbah.MongoCollection

final class ForumEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    userRepo: UserRepo) {

  import settings._

  lazy val categRepo = new CategRepo(mongodb(MongoCollectionForumCateg))

  lazy val categApi = new CategApi(this)
}
