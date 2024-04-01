package lila.notify

import akka.actor.*
import akka.stream.Materializer
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.Bus
import lila.common.config.*
import lila.db.dsl.Coll
import lila.core.config.CollName

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    getLightUser: lila.core.LightUser.Getter,
    getLightUserSync: lila.core.LightUser.GetterSync,
    cacheApi: lila.memo.CacheApi,
    subsRepo: lila.core.relation.SubscriptionRepo,
    jsDump: lila.core.i18n.JsDump,
    langPicker: lila.core.i18n.LangPicker
)(using Executor, ActorSystem, Materializer, lila.core.i18n.Translator):

  lazy val jsonHandlers = wire[JSONHandlers]

  val colls = NotifyColls(notif = db(CollName("notify")), pref = db(CollName("notify_pref")))

  private val maxPerPage = MaxPerPage(7)

  private lazy val repo = wire[NotificationRepo]

  lazy val api = wire[NotifyApi]

  val getAllows = GetNotifyAllows(api.prefs.allows)

  // api actor
  Bus.subscribeFuns(
    "notify" -> {
      case lila.core.actorApi.notify.NotifiedBatch(userIds) => api.markAllRead(userIds)
      case lila.game.actorApi.CorresAlarmEvent(pov) =>
        pov.player.userId.so: userId =>
          lila.game.Namer
            .playerText(pov.opponent)(using getLightUser)
            .foreach: opponent =>
              api.notifyOne(userId, CorresAlarm(gameId = pov.gameId, opponent = opponent))
    },
    "streamStart" -> { case lila.core.actorApi.streamer.StreamStart(userId, streamerName) =>
      subsRepo
        .subscribersOnlineSince(userId, 7)
        .map: subs =>
          api.notifyMany(subs, StreamStart(userId, streamerName))
    }
  )

  lazy val cli = wire[NotifyCli]

final class NotifyColls(val notif: Coll, val pref: Coll)

private type GetNotifyAllowsType                   = (UserId, NotificationPref.Event) => Fu[Allows]
opaque type GetNotifyAllows <: GetNotifyAllowsType = GetNotifyAllowsType
object GetNotifyAllows extends TotalWrapper[GetNotifyAllows, GetNotifyAllowsType]
