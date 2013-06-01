package lila.relation

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    getOnlineUserIds: () ⇒ Set[String],
    getUsername: String ⇒ Fu[String],
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CollectionRelation = config getString "collection.relation"
    val ActorNotifyFreq = config duration "actor.notify_freq"
    val ActorResyncFreq = config duration "actor.resync_freq"
    val ActorName = config getString "actor.name"
  }
  import settings._

  lazy val api = new RelationApi(
    cached = cached,
    actor = hub.actor.relation,
    getOnlineUserIds = getOnlineUserIds,
    timeline = hub.actor.timeline)

  private lazy val cached = new Cached

  private[relation] val actor = system.actorOf(Props(new RelationActor(
    socketHub = hub.socket.hub,
    getOnlineUserIds = getOnlineUserIds,
    getUsername = getUsername,
    api = api
  )), name = ActorName)

  {
    import scala.concurrent.duration._
    import makeTimeout.short
    import lila.hub.actorApi.WithUserIds

    scheduler.message(ActorNotifyFreq) {
      actor -> actorApi.NotifyMovement
    }

    scheduler.message(ActorResyncFreq) {
      actor -> actorApi.ReloadAllOnlineFriends
    }
  }

  private[relation] lazy val relationColl = db(CollectionRelation)
}

object Env {

  private def app = play.api.Play.current

  lazy val current = "[boot] relation" describes new Env(
    config = lila.common.PlayApp loadConfig "relation",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    getOnlineUserIds = () ⇒ lila.user.Env.current.onlineUserIdMemo.keySet,
    getUsername = lila.user.Env.current.usernameOrAnonymous,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
