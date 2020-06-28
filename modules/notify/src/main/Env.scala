package lila.notify

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.Bus
import lila.common.config._

private class NotifyConfig(
    @ConfigName("collection.notify") val notifyColl: CollName,
    @ConfigName("actor.name") val actorName: String
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    getLightUser: lila.common.LightUser.Getter,
    getLightUserSync: lila.common.LightUser.GetterSync,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[NotifyConfig]("notify")(AutoConfig.loader)

  lazy val jsonHandlers = wire[JSONHandlers]

  private lazy val repo = new NotificationRepo(coll = db(config.notifyColl))

  private val maxPerPage = MaxPerPage(7)

  lazy val api = wire[NotifyApi]

  // api actor
  Bus.subscribe(
    system.actorOf(
      Props(new Actor {
        def receive = {
          case lila.hub.actorApi.notify.Notified(userId) =>
            api markAllRead Notification.Notifies(userId)
          case lila.hub.actorApi.notify.NotifiedBatch(userIds) =>
            api markAllRead userIds.map(Notification.Notifies.apply)
          case lila.game.actorApi.CorresAlarmEvent(pov) =>
            pov.player.userId ?? { userId =>
              lila.game.Namer.playerText(pov.opponent)(getLightUser) foreach { opponent =>
                api addNotification Notification.make(
                  Notification.Notifies(userId),
                  CorresAlarm(
                    gameId = pov.gameId,
                    opponent = opponent
                  )
                )
              }
            }
        }
      }),
      name = config.actorName
    ),
    "corresAlarm"
  )
}
