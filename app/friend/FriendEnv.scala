package lila
package friend

import core.Settings
import user.UserRepo
import message.LichessThread

import com.mongodb.casbah.MongoCollection
import scalaz.effects._

final class FriendEnv(
    settings: Settings,
    userRepo: UserRepo,
    sendMessage: LichessThread ⇒ IO[Unit],
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val friendRepo = new FriendRepo(mongodb(FriendCollectionFriend))

  lazy val requestRepo = new RequestRepo(mongodb(FriendCollectionRequest))

  lazy val api = new FriendApi(
    friendRepo = friendRepo,
    requestRepo = requestRepo,
    userRepo = userRepo,
    cached = cached)

  lazy val forms = new DataForm(friendRepo)

  lazy val cached = new Cached(friendRepo, requestRepo)
}
