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

  lazy val api = new Api(
    threadRepo = threadRepo,
    maxPerPage = MessageThreadMaxPerPage)

  lazy val unread = new UnreadCache(threadRepo)
  
  lazy val forms = DataForm
}
