package lila.friend

import lila.common.PimpedConfig._

import com.typesafe.config.Config
import akka.actor._
import akka.pattern.pipe

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    getOnlineUserIds: () ⇒ Set[String],
    getUsername: String ⇒ Fu[String],
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CollectionFriend = config getString "collection.friend"
    val CollectionRequest = config getString "collection.request"
    val ActorNotifyFreq = config duration "actor.notify_freq"
    val ActorName = config getString "actor.name"
  }
  import settings._

  lazy val api = new FriendApi(cached = cached)

  private lazy val cached = new Cached

  val friendIds = cached.friendIds apply _

  def cli = new lila.common.Cli {
    def process = {
      case "friend" :: u1 :: u2 :: Nil ⇒ api.makeFriends(u1, u2) inject "done"
    }
  }

  private[friend] val actor = system.actorOf(Props(new FriendActor(
    socketHub = hub.socket.hub,
    getOnlineUserIds = getOnlineUserIds,
    getUsername = getUsername,
    getFriendIds = cached.friendIds.apply
  )), name = ActorName)

  {
    import scala.concurrent.duration._
    import makeTimeout.short
    import lila.hub.actorApi.WithUserIds

    scheduler.message(ActorNotifyFreq) {
      actor -> actorApi.NotifyMovement
    }
  }

  private[friend] lazy val friendColl = db(CollectionFriend)
  private[friend] lazy val requestColl = db(CollectionRequest)
}

object Env {

  private def app = play.api.Play.current

  lazy val current = "[boot] friend" describes new Env(
    config = lila.common.PlayApp loadConfig "friend",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    getOnlineUserIds = () ⇒ lila.user.Env.current.onlineUserIdMemo.keySet,
    getUsername = lila.user.Env.current.usernameOrAnonymous,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
