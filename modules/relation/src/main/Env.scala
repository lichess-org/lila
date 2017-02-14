package lila.relation

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    getOnlineUserIds: () => Set[ID],
    lightUser: lila.common.LightUser.Getter,
    lightUserSync: lila.common.LightUser.GetterSync,
    followable: String => Fu[Boolean],
    system: ActorSystem,
    asyncCache: lila.memo.AsyncCache.Builder,
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
    asyncCache = asyncCache,
    maxFollow = MaxFollow,
    maxBlock = MaxBlock)

  val onlinePlayings = new lila.memo.ExpireSetMemo(4 hour)

  private val onlineStudying: Cache[ID, String] /* userId, studyId */ =
    Scaffeine().expireAfterAccess(20 minutes).build[ID, String] // people with write access in public studies

  val currentlyStudying: ID => Option[String] = onlineStudying.getIfPresent

  private val onlineStudyingAll = new lila.memo.ExpireSetMemo(20 minutes) // people with write or read access in public and private studies

  private[relation] val actor = system.actorOf(Props(new RelationActor(
    getOnlineUserIds = getOnlineUserIds,
    lightUser = lightUser,
    api = api,
    onlinePlayings,
    onlineStudying,
    onlineStudyingAll
  )), name = ActorName)

  scheduler.once(15 seconds) {
    scheduler.message(ActorNotifyFreq) {
      actor -> actorApi.ComputeMovement
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
    lightUserSync = lila.user.Env.current.lightUserSync,
    followable = lila.pref.Env.current.api.followable _,
    system = lila.common.PlayApp.system,
    asyncCache = lila.memo.Env.current.asyncCache,
    scheduler = lila.common.PlayApp.scheduler)
}
