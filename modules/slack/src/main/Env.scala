package lila.slack

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.hub.actorApi.{ DonationEvent, Deploy, RemindDeployPre, RemindDeployPost }

final class Env(
    config: Config,
    getLightUser: String => Option[lila.common.LightUser],
    system: ActorSystem) {

  private val IncomingUrl = config getString "incoming.url"
  private val IncomingDefaultChannel = config getString "incoming.default_channel"

  lazy val api = new SlackApi(client, getLightUser)

  private lazy val client = new SlackClient(
    url = IncomingUrl,
    defaultChannel = IncomingDefaultChannel)

  system.actorOf(Props(new Actor {
    override def preStart() {
      system.lilaBus.subscribe(self, 'donation, 'deploy)
    }
    def receive = {
      case d: DonationEvent            => api donation d
      case Deploy(RemindDeployPre, _)  => api.deployPre
      case Deploy(RemindDeployPost, _) => api.deployPost
    }
  }))
}

object Env {

  lazy val current: Env = "slack" boot new Env(
    system = lila.common.PlayApp.system,
    getLightUser = lila.user.Env.current.lightUser,
    config = lila.common.PlayApp loadConfig "slack")
}
