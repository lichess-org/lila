package lila.timeline

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.core.config.*

@Module
private class TimelineConfig(
    @ConfigName("collection.entry") val entryColl: CollName,
    @ConfigName("collection.unsub") val unsubColl: CollName,
    @ConfigName("user.display_max") val userDisplayMax: Max
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    relationApi: lila.core.relation.RelationApi,
    cacheApi: lila.memo.CacheApi,
    teamApi: lila.core.team.TeamApi
)(using Executor):

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
    unsubApi.get(channel, me).flatMap {
      if _ then fuccess(Some(true)) // unsubbed
      else
        entryApi.channelUserIdRecentExists(channel, me).map {
          if _ then Some(false) // subbed
          else None             // not applicable
        }
    }

  private val api = wire[TimelineApi]

  lila.common.Bus.subscribeFuns(
    "shadowban" -> { case lila.core.mod.Shadowban(userId, true) =>
      entryApi.removeRecentFollowsBy(userId)
    },
    "timeline" -> { case propagate: lila.core.timeline.Propagate => api(propagate) }
  )
