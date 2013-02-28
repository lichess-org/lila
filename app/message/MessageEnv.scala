package lila.app
package message

import user.{ User, UserRepo }
import core.Settings

import com.mongodb.casbah.MongoCollection

final class MessageEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    userRepo: UserRepo,
    notifyUnread: (String, Int) ⇒ Unit) {

  import settings._

  lazy val threadRepo = new ThreadRepo(mongodb(MessageCollectionThread))

  lazy val unreadCache = new UnreadCache(threadRepo)

  lazy val api = new Api(
    threadRepo = threadRepo,
    unreadCache = unreadCache,
    userRepo = userRepo,
    maxPerPage = MessageThreadMaxPerPage,
    notifyUnread = notifyUnread)

  lazy val forms = new DataForm(userRepo)
}
