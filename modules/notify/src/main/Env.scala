package lila.notify

import akka.actor.ActorSystem
import com.typesafe.config.Config

final class Env(
    db: lila.db.Env,
    config: Config,
    getLightUser: lila.common.LightUser.Getter,
    system: ActorSystem) {

  val settings = new {
    val collectionNotifications = config getString "collection.notify"

  }

  import settings._

  val jsonHandlers = new JSONHandlers(getLightUser)

  private lazy val repo = new NotificationRepo(coll = db(collectionNotifications))

  lazy val notifyApi = new NotifyApi(
    bus = system.lilaBus,
    jsonHandlers = jsonHandlers,
    repo = repo)
}

object Env {

  lazy val current = "notify" boot new Env(db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "notify",
    getLightUser = lila.user.Env.current.lightUser,
    system = lila.common.PlayApp.system)

}
