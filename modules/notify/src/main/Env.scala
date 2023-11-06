package lila.notify

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration
import akka.stream.Materializer

import lila.db.dsl.Coll
import lila.common.Bus
import lila.common.config.*

@Module
@annotation.nowarn("msg=unused")
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    getLightUser: lila.common.LightUser.Getter,
    getLightUserSync: lila.common.LightUser.GetterSync,
    cacheApi: lila.memo.CacheApi,
    prefApi: lila.pref.PrefApi,
    subsRepo: lila.relation.SubscriptionRepo
)(using Executor, ActorSystem, Materializer):

  lazy val jsonHandlers = wire[JSONHandlers]

  val colls = NotifyColls(notif = db(CollName("notify")), pref = db(CollName("notify_pref")))

  private val maxPerPage = MaxPerPage(7)

  private lazy val repo = wire[NotificationRepo]

  lazy val api = wire[NotifyApi]

  val getAllows = GetNotifyAllows(api.prefs.allows)

  // api actor
  Bus.subscribeFuns(
    "notify" -> {
      case lila.hub.actorApi.notify.NotifiedBatch(userIds) => api.markAllRead(userIds)
      case lila.game.actorApi.CorresAlarmEvent(pov) =>
        pov.player.userId.so: userId =>
          lila.game.Namer
            .playerText(pov.opponent)(using getLightUser)
            .foreach: opponent =>
              api.notifyOne(userId, CorresAlarm(gameId = pov.gameId, opponent = opponent))
    },
    "streamStart" -> { case lila.hub.actorApi.streamer.StreamStart(userId, streamerName) =>
      subsRepo.subscribersOnlineSince(userId, 7) map { subs =>
        api.notifyMany(subs, StreamStart(userId, streamerName))
      }
    }
  )

  lazy val cli = wire[NotifyCli]

final class NotifyColls(val notif: Coll, val pref: Coll)

private type GetNotifyAllowsType                   = (UserId, NotificationPref.Event) => Fu[Allows]
opaque type GetNotifyAllows <: GetNotifyAllowsType = GetNotifyAllowsType
object GetNotifyAllows extends TotalWrapper[GetNotifyAllows, GetNotifyAllowsType]
