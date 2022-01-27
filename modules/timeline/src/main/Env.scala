package lila.timeline

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._

@Module
private class TimelineConfig(
    @ConfigName("collection.entry") val entryColl: CollName,
    @ConfigName("collection.unsub") val unsubColl: CollName,
    @ConfigName("user.display_max") val userDisplayMax: Max,
    @ConfigName("user.actor.name") val userActorName: String
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    relationApi: lila.relation.RelationApi,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[TimelineConfig]("timeline")(AutoConfig.loader)

  lazy val entryApi = new EntryApi(
    coll = db(config.entryColl),
    cacheApi = cacheApi,
    userMax = config.userDisplayMax
  )

  lazy val unsubApi = new UnsubApi(db(config.unsubColl))

  def isUnsub(channel: String)(userId: String): Fu[Boolean] =
    unsubApi.get(channel, userId)

  def status(channel: String)(userId: String): Fu[Option[Boolean]] =
    unsubApi.get(channel, userId) flatMap {
      case true => fuccess(Some(true)) // unsubed
      case false =>
        entryApi.channelUserIdRecentExists(channel, userId) map {
          case true  => Some(false) // subed
          case false => None        // not applicable
        }
    }

  system.actorOf(Props(wire[TimelinePush]), name = config.userActorName)

  lila.common.Bus.subscribeFun("shadowban") { case lila.hub.actorApi.mod.Shadowban(userId, true) =>
    entryApi.removeRecentFollowsBy(userId).unit
  }
}
