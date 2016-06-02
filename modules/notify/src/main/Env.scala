package lila.notify

import akka.actor._
import com.typesafe.config.Config

final class Env(
    db: lila.db.Env,
    config: Config,
    getLightUser: lila.common.LightUser.Getter,
    system: ActorSystem) {

  private val CollectionNotifications = config getString "collection.notify"
  private val ActorName = config getString "actor.name"

  val jsonHandlers = new JSONHandlers(getLightUser)

  private lazy val repo = new NotificationRepo(coll = db(CollectionNotifications))

  lazy val api = new NotifyApi(
    bus = system.lilaBus,
    jsonHandlers = jsonHandlers,
    repo = repo)

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.notify.Notified(userId) =>
        api markAllRead Notification.Notifies(userId)
    }
  }), name = ActorName)
}

object Env {

  lazy val current = "notify" boot new Env(db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "notify",
    getLightUser = lila.user.Env.current.lightUser,
    system = lila.common.PlayApp.system)

}
