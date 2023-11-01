package lila.timeline

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*
import lila.user.Me

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
    cacheApi: lila.memo.CacheApi,
    memberRepo: lila.team.TeamMemberRepo,
    teamCache: lila.team.Cached
)(using
    ec: Executor,
    system: ActorSystem
):

  private val config = appConfig.get[TimelineConfig]("timeline")(AutoConfig.loader)

  lazy val entryApi = EntryApi(
    coll = db(config.entryColl),
    cacheApi = cacheApi,
    userMax = config.userDisplayMax
  )

  lazy val unsubApi = UnsubApi(db(config.unsubColl))

  def isUnsub(channel: String)(using me: Me): Fu[Boolean] =
    unsubApi.get(channel, me)

  def status(channel: String)(using me: Me): Fu[Option[Boolean]] =
    unsubApi.get(channel, me) flatMap {
      if _ then fuccess(Some(true)) // unsubbed
      else
        entryApi.channelUserIdRecentExists(channel, me) map {
          if _ then Some(false) // subbed
          else None             // not applicable
        }
    }

  system.actorOf(Props(wire[TimelinePush]), name = config.userActorName)

  lila.common.Bus.subscribeFun("shadowban") { case lila.hub.actorApi.mod.Shadowban(userId, true) =>
    entryApi.removeRecentFollowsBy(userId)
  }
