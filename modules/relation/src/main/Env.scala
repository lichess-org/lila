package lila.relation

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.hub.actors

@Module
private class RelationConfig(
    @ConfigName("collection.relation") val collection: CollName,
    @ConfigName("actor.notify_freq") val actorNotifyFreq: FiniteDuration,
    @ConfigName("actor.name") val actorName: String,
    @ConfigName("limit.follow") val maxFollow: Max,
    @ConfigName("limit.block") val maxBlock: Max
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    relation: actors.Relation,
    timeline: actors.Timeline,
    userRepo: lila.user.UserRepo,
    onlineUserIds: lila.socket.OnlineIds,
    lightUserSync: lila.common.LightUser.GetterSync,
    prefApi: lila.pref.PrefApi,
    asyncCache: lila.memo.AsyncCache.Builder
)(implicit ec: scala.concurrent.ExecutionContext, system: ActorSystem) {

  private val config = appConfig.get[RelationConfig]("relation")(AutoConfig.loader)

  def maxFollow = config.maxFollow

  private lazy val coll = db(config.collection)

  private lazy val repo: RelationRepo = wire[RelationRepo]

  lazy val api: RelationApi = wire[RelationApi]

  lazy val stream = wire[RelationStream]

  lazy val online: OnlineDoing = wire[OnlineDoing]

  def isPlaying(userId: lila.user.User.ID): Boolean = online.playing.get(userId)

  private val actor = system.actorOf(Props(wire[RelationActor]), name = config.actorName)

  system.scheduler.scheduleWithFixedDelay(15 seconds, config.actorNotifyFreq) { () =>
    actor ! actorApi.ComputeMovement
  }
}
