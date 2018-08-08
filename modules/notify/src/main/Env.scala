package lidraughts.notify

import akka.actor._
import com.typesafe.config.Config

final class Env(
    db: lidraughts.db.Env,
    config: Config,
    getLightUser: lidraughts.common.LightUser.GetterSync,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    system: ActorSystem
) {

  private val CollectionNotifications = config getString "collection.notify"
  private val ActorName = config getString "actor.name"

  val jsonHandlers = new JSONHandlers(getLightUser)

  private lazy val repo = new NotificationRepo(coll = db(CollectionNotifications))

  lazy val api = new NotifyApi(
    bus = system.lidraughtsBus,
    jsonHandlers = jsonHandlers,
    repo = repo,
    asyncCache = asyncCache
  )

  // api actor
  system.lidraughtsBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.hub.actorApi.notify.Notified(userId) =>
        api markAllRead Notification.Notifies(userId)
      case lidraughts.game.actorApi.CorresAlarmEvent(pov) => pov.player.userId ?? { userId =>
        api addNotification Notification.make(
          Notification.Notifies(userId),
          CorresAlarm(
            gameId = pov.gameId,
            opponent = lidraughts.game.Namer.playerText(pov.opponent)(getLightUser)
          )
        )
      }
    }
  }), name = ActorName), 'corresAlarm)
}

object Env {

  lazy val current = "notify" boot new Env(
    db = lidraughts.db.Env.current,
    config = lidraughts.common.PlayApp loadConfig "notify",
    getLightUser = lidraughts.user.Env.current.lightUserSync,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    system = lidraughts.common.PlayApp.system
  )

}
