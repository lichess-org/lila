package lila.notify

import akka.actor.ActorSystem
import com.typesafe.config.Config

final class Env(db: lila.db.Env, config: Config, system: ActorSystem) {

  val settings = new {
    val collectionNotifications = config getString "collection.notify"

  }

  import settings._

  private lazy val repo = new NotificationRepo(coll = db(collectionNotifications))

  lazy val notifyApi = new NotifyApi(bus = system.lilaBus, repo = repo)
}

object Env {

  lazy val current = "notify" boot new Env(db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "notify",
    system = lila.common.PlayApp.system)

}