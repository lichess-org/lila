package lila.notify

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.Bus
import lila.common.config._

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
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  lazy val jsonHandlers = wire[JSONHandlers]

  private lazy val notifyColl = db(CollName("notify"))

  private val maxPerPage = MaxPerPage(7)

  private lazy val repo = wire[NotificationRepo]

  lazy val api = wire[NotifyApi]

  // api actor
  Bus.subscribeFun("notify") {
    case lila.hub.actorApi.notify.NotifiedBatch(userIds) =>
      api.markAllRead(userIds).unit
    case lila.game.actorApi.CorresAlarmEvent(pov) =>
      pov.player.userId ?? { userId =>
        lila.game.Namer.playerText(pov.opponent)(getLightUser) foreach { opponent =>
          api notifyOne (
            userId,
            CorresAlarm(
              gameId = pov.gameId,
              opponent = opponent
            )
          )
        }
      }
  }
}
