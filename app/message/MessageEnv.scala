package lila
package message

import user.{ User, UserRepo }
import core.Settings

import com.mongodb.casbah.MongoCollection

final class MessageEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    userRepo: UserRepo) {

  import settings._

  lazy val threadRepo = new ThreadRepo(mongodb(MongoCollectionMessageThread))

  lazy val unreadCache = new UnreadCache(threadRepo)

  lazy val api = new Api(
    threadRepo = threadRepo,
    unreadCache = unreadCache,
    maxPerPage = MessageThreadMaxPerPage)
  
  lazy val forms = new DataForm(userRepo)
}
