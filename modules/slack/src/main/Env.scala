package lila.slack

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    getLightUser: String => Option[lila.common.LightUser],
    system: ActorSystem) {

  private val IncomingUrl = config getString "incoming.url"

  private lazy val api = new SlackApi(client, getLightUser)

  private lazy val client = new SlackClient(url = IncomingUrl)

  system.actorOf(Props(new Actor {
    override def preStart() {
      system.lilaBus.subscribe(self, 'donation)
    }
    import akka.pattern.pipe
    def receive = {
      case d: lila.hub.actorApi.DonationEvent => api donation d
    }
  }))
}

object Env {

  lazy val current: Env = "slack" boot new Env(
    system = lila.common.PlayApp.system,
    getLightUser = lila.user.Env.current.lightUser,
    config = lila.common.PlayApp loadConfig "slack")
}
