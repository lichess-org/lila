package lila.irwin

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env
) {

  private val reportColl = db(config getString "collection.report")
  private val requestColl = db(config getString "collection.request")

  val api = new IrwinApi(
    reportColl = reportColl,
    requestColl = requestColl
  )

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.report.Created(userId, "cheat") => api.requests.insert(userId, _.Report)
      case lila.hub.actorApi.report.Processed(userId, "cheat") => api.requests.drop(userId)
    }
  })), 'report)
}

object Env {

  lazy val current: Env = "irwin" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "irwin",
    system = lila.common.PlayApp.system
  )
}
