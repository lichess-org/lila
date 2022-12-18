package lila.notify

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.*
import play.api.Configuration

import lila.db.dsl.Coll
import lila.common.Bus
import lila.common.config.*

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    getLightUser: lila.common.LightUser.Getter,
    getLightUserSync: lila.common.LightUser.GetterSync,
    cacheApi: lila.memo.CacheApi,
    prefApi: lila.pref.PrefApi,
    subsRepo: lila.relation.SubscriptionRepo
)(using scala.concurrent.ExecutionContext, ActorSystem):

  lazy val jsonHandlers = wire[JSONHandlers]

  private val colls = new NotifyColls(notif = db(CollName("notify")), pref = db(CollName("notify_pref")))

  private val maxPerPage = MaxPerPage(7)

  private lazy val repo = wire[NotificationRepo]

  lazy val api = wire[NotifyApi]

  val getAllows = GetNotifyAllows(api.prefs.allows)

  // api actor
  Bus.subscribeFun("notify") {
    case lila.hub.actorApi.notify.NotifiedBatch(userIds) =>
      api.markAllRead(userIds) unit
    case lila.game.actorApi.CorresAlarmEvent(pov) =>
      pov.player.userId ?? { userId =>
        lila.game.Namer.playerText(pov.opponent)(using getLightUser) foreach { opponent =>
          api notifyOne (
            userId,
            CorresAlarm(gameId = pov.gameId, opponent = opponent)
          )
        }
      }
    case lila.hub.actorApi.streamer.StreamStart(userId, streamerName) =>
      subsRepo.subscribersOnlineSince(userId, 7) map { subs =>
        api.notifyMany(subs, StreamStart(userId, streamerName))
      }
  }

final private class NotifyColls(val notif: Coll, val pref: Coll)

private type GetNotifyAllowsType                   = (UserId, NotificationPref.Event) => Fu[Allows]
opaque type GetNotifyAllows <: GetNotifyAllowsType = GetNotifyAllowsType
object GetNotifyAllows extends TotalWrapper[GetNotifyAllows, GetNotifyAllowsType]
