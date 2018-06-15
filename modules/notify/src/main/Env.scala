package lila.notify

import akka.actor._
import com.typesafe.config.Config

final class Env(
    db: lila.db.Env,
    config: Config,
    getLightUser: lila.common.LightUser.GetterSync,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: ActorSystem
) {

  private val CollectionNotifications = config getString "collection.notify"
  private val ActorName = config getString "actor.name"

  val jsonHandlers = new JSONHandlers(getLightUser)

  private lazy val repo = new NotificationRepo(coll = db(CollectionNotifications))

  lazy val api = new NotifyApi(
    bus = system.lilaBus,
    jsonHandlers = jsonHandlers,
    repo = repo,
    asyncCache = asyncCache
  )

  // api actor
  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.notify.Notified(userId) =>
        api markAllRead Notification.Notifies(userId)
      case lila.game.actorApi.CorresAlarmEvent(pov) => pov.player.userId ?? { userId =>
        api addNotification Notification.make(
          Notification.Notifies(userId),
          CorresAlarm(
            gameId = pov.gameId,
            opponent = lila.game.Namer.playerText(pov.opponent)(getLightUser)
          )
        )
      }
    }
  }), name = ActorName), 'corresAlarm)
}

object Env {

  lazy val current = "notify" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "notify",
    getLightUser = lila.user.Env.current.lightUserSync,
    asyncCache = lila.memo.Env.current.asyncCache,
    system = lila.common.PlayApp.system
  )

}
