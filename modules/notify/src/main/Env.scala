package lila.notify

import akka.actor.*
import akka.stream.Materializer
import com.softwaremill.macwire.*
import lila.common.Bus
import lila.common.config.*
import lila.db.dsl.Coll
import lila.core.config.CollName
import lila.core.notify.{ NotificationContent, GetNotifyAllows }

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.core.user.UserRepo,
    userApi: lila.core.user.UserApi,
    getLightUserSync: lila.core.LightUser.GetterSync,
    cacheApi: lila.memo.CacheApi,
    subsRepo: lila.core.relation.SubscriptionRepo,
    langPicker: lila.core.i18n.LangPicker
)(using Executor, ActorSystem, Materializer, lila.core.i18n.Translator):

  lazy val jsonHandlers = wire[JSONHandlers]

  val colls = NotifyColls(notif = db(CollName("notify")), pref = db(CollName("notify_pref")))

  private val maxPerPage = MaxPerPage(7)

  private lazy val repo = wire[NotificationRepo]

  lazy val api = wire[NotifyApi]

  val getAllows = GetNotifyAllows(api.prefs.allows)

  // api actor
  Bus.subscribeFun("notify"):
    case lila.core.notify.NotifiedBatch(userIds) => api.markAllRead(userIds)
    case lila.core.game.CorresAlarmEvent(userId, pov, opponent) =>
      api.notifyOne(userId, NotificationContent.CorresAlarm(pov.game.id, opponent))

  Bus.sub[lila.core.misc.streamer.StreamStart]:
    case lila.core.misc.streamer.StreamStart(userId, streamerName) =>
      subsRepo
        .subscribersOnlineSince(userId, 7)
        .map: subs =>
          api.notifyMany(subs, NotificationContent.StreamStart(userId, streamerName))

  lazy val cli = wire[NotifyCli]

final class NotifyColls(val notif: Coll, val pref: Coll)
