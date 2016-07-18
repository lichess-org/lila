package lila.relation

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    getOnlineUserIds: () => Set[String],
    lightUser: String => Option[lila.common.LightUser],
    followable: String => Fu[Boolean],
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CollectionRelation = config getString "collection.relation"
    val ActorNotifyFreq = config duration "actor.notify_freq"
    val ActorName = config getString "actor.name"
    val MaxFollow = config getInt "limit.follow"
    val MaxBlock = config getInt "limit.block"
  }
  import settings._

  private[relation] val coll = db(CollectionRelation)

  lazy val api = new RelationApi(
    coll = coll,
    actor = hub.actor.relation,
    bus = system.lilaBus,
    timeline = hub.actor.timeline,
    reporter = hub.actor.report,
    followable = followable,
    maxFollow = MaxFollow,
    maxBlock = MaxBlock)

  private[relation] val actor = system.actorOf(Props(new RelationActor(
    getOnlineUserIds = getOnlineUserIds,
    lightUser = lightUser,
    api = api
  )), name = ActorName)

  scheduler.once(15 seconds) {
    scheduler.message(ActorNotifyFreq) {
      actor -> actorApi.NotifyMovement
    }
  }
}

object Env {

  lazy val current = "relation" boot new Env(
    config = lila.common.PlayApp loadConfig "relation",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    getOnlineUserIds = () => lila.user.Env.current.onlineUserIdMemo.keySet,
    lightUser = lila.user.Env.current.lightUser,
    followable = lila.pref.Env.current.api.followable _,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
