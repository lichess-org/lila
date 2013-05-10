package lila.tournament

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._
import akka.pattern.pipe

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    hub: lila.hub.Env,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val OrganizerName = config getString "organizer.name"
    val CollectionTournament = config getString "collection.tournament"
    val CollectionRoom = config getString "collection.room"
    val MessageTtl = config duration "message.ttl"
    val MemoTtl = config duration "memo.ttl"
    val UidTimeout = config duration "uid.timeout"
    val HubTimeout = config duration "hub.timeout"
  }
  import settings._

  private[tournament] lazy val tournamentColl = db(CollectionTournament)
}

object Env {

  private def app = play.api.Play.current

  lazy val current = "[boot] tournamen" describes new Env(
    config = lila.common.PlayApp loadConfig "tournamen",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    scheduler = lila.common.PlayApp.scheduler)
}
