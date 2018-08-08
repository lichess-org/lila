package lidraughts.relation

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    onlineUserIds: lidraughts.memo.ExpireSetMemo,
    lightUserApi: lidraughts.user.LightUserApi,
    followable: String => Fu[Boolean],
    system: ActorSystem,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    scheduler: lidraughts.common.Scheduler
) {

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
    bus = system.lidraughtsBus,
    timeline = hub.actor.timeline,
    reporter = hub.actor.report,
    followable = followable,
    asyncCache = asyncCache,
    maxFollow = MaxFollow,
    maxBlock = MaxBlock
  )

  val online = new OnlineDoing(
    api,
    lightUser = lightUserApi.sync,
    onlineUserIds
  )

  private[relation] val actor = system.actorOf(Props(new RelationActor(
    lightUser = lightUserApi.sync,
    api = api,
    online = online
  )), name = ActorName)

  scheduler.once(15 seconds) {
    scheduler.message(ActorNotifyFreq) {
      actor -> actorApi.ComputeMovement
    }
  }
}

object Env {

  lazy val current = "relation" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "relation",
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    onlineUserIds = lidraughts.user.Env.current.onlineUserIdMemo,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    followable = lidraughts.pref.Env.current.api.followable _,
    system = lidraughts.common.PlayApp.system,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    scheduler = lidraughts.common.PlayApp.scheduler
  )
}
