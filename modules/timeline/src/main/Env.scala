package lila.timeline

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*

@Module
private class TimelineConfig(
    @ConfigName("collection.entry") val entryColl: CollName,
    @ConfigName("collection.unsub") val unsubColl: CollName,
    @ConfigName("user.display_max") val userDisplayMax: Max,
    @ConfigName("user.actor.name") val userActorName: String
)

@Module
@annotation.nowarn("msg=unused")
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    relationApi: lila.relation.RelationApi,
    cacheApi: lila.memo.CacheApi,
    memberRepo: lila.team.MemberRepo,
    teamCache: lila.team.Cached
)(using
    ec: Executor,
    system: ActorSystem
):

  private val config = appConfig.get[TimelineConfig]("timeline")(AutoConfig.loader)

  lazy val entryApi = new EntryApi(
    coll = db(config.entryColl),
    cacheApi = cacheApi,
    userMax = config.userDisplayMax
  )

  lazy val unsubApi = new UnsubApi(db(config.unsubColl))

  def isUnsub(channel: String)(userId: UserId): Fu[Boolean] =
    unsubApi.get(channel, userId)

  def status(channel: String)(userId: UserId): Fu[Option[Boolean]] =
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
