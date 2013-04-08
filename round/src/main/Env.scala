package lila.round

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    hub: lila.hub.Env) {

  private val settings = new {
    val MessageLifetime = config duration "message.lifetime"
    val UidTimeout = config duration "uid.timeout"
    val HubTimeout = config duration "hub.timeout"
    val PlayerTimeout = config duration "player.timeout"
    val AnimationDelay = config duration "animation.delay"
    val Moretime = config seconds "moretime"
    val CollectionRoom = config getString "collection.room"
    val CollectionWatcherRoom = config getString "collection.watcher_room"
  }
  import settings._

  private[round] lazy val roomColl = db(CollectionRoom)

  private[round] lazy val watcherRoomColl = db(CollectionWatcherRoom)
}

object Env {

  lazy val current = "[boot] round" describes new Env(
    config = lila.common.PlayApp loadConfig "round",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    hub = lila.hub.Env.current)
}
