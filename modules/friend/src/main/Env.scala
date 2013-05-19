package lila.friend

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._
import akka.pattern.pipe

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env) {

  private val CollectionFriend = config getString "collection.friend"
  private val CollectionRequest = config getString "collection.request"

  // lazy val api = new FriendApi(
  //   friendRepo = friendRepo,
  //   requestRepo = requestRepo,
  //   userRepo = userRepo,
  //   cached = cached)

  lazy val forms = new DataForm

  lazy val cached = new Cached

  private[friend] lazy val friendColl = db(CollectionFriend)
  private[friend] lazy val requestColl = db(CollectionRequest)
}

object Env {

  private def app = play.api.Play.current

  lazy val current = "[boot] friend" describes new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current)
}
