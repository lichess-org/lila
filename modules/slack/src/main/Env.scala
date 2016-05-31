package lila.slack

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.hub.actorApi.slack.Event
import lila.hub.actorApi.{ DonationEvent, DeployPre, DeployPost }

final class Env(
    config: Config,
    getLightUser: String => Option[lila.common.LightUser],
    system: ActorSystem) {

  private val IncomingUrl = config getString "incoming.url"
  private val IncomingDefaultChannel = config getString "incoming.default_channel"
  private val NetDomain = config getString "domain"

  private val isProd = NetDomain == "lichess.org"

  lazy val api = new SlackApi(client, isProd, getLightUser)

  private lazy val client = new SlackClient(
    url = IncomingUrl,
    defaultChannel = IncomingDefaultChannel)

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case d: DonationEvent => api donation d
      case DeployPre        => api.deployPre
      case DeployPost       => api.deployPost
      case e: Event         => api publishEvent e
    }
  })), 'donation, 'deploy, 'slack)
}

object Env {

  lazy val current: Env = "slack" boot new Env(
    system = lila.common.PlayApp.system,
    getLightUser = lila.user.Env.current.lightUser,
    config = lila.common.PlayApp loadConfig "slack")
}
