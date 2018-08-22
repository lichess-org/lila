package lila.slack

import akka.actor._
import com.typesafe.config.Config

import lila.hub.actorApi.plan.ChargeEvent
import lila.hub.actorApi.slack.Event
import lila.hub.actorApi.user.Note
import lila.hub.actorApi.{ DeployPre, DeployPost }

final class Env(
    config: Config,
    getLightUser: lila.common.LightUser.Getter,
    system: ActorSystem
) {

  private val IncomingUrl = config getString "incoming.url"
  private val IncomingDefaultChannel = config getString "incoming.default_channel"
  private val NetDomain = config getString "domain"

  private val isProd = NetDomain == "lichess.org"

  lazy val api = new SlackApi(client, isProd, getLightUser)

  private lazy val client = new SlackClient(
    url = IncomingUrl,
    defaultChannel = IncomingDefaultChannel
  )

  system.lilaBus.subscribeFun('deploy, 'slack, 'plan, 'userNote) {
    case d: ChargeEvent => api charge d
    case DeployPre => api.deployPre
    case DeployPost => api.deployPost
    case Note(from, to, text, true) if from != "Irwin" => api.userModNote(from, to, text)
    case e: Event => api publishEvent e
  }
}

object Env {

  lazy val current: Env = "slack" boot new Env(
    system = lila.common.PlayApp.system,
    getLightUser = lila.user.Env.current.lightUser,
    config = lila.common.PlayApp loadConfig "slack"
  )
}
