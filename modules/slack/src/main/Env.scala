package lidraughts.slack

import akka.actor._
import com.typesafe.config.Config

import lidraughts.hub.actorApi.plan.ChargeEvent
import lidraughts.hub.actorApi.slack.Event
import lidraughts.hub.actorApi.user.Note
import lidraughts.hub.actorApi.{ DeployPre, DeployPost }

final class Env(
    config: Config,
    getLightUser: lidraughts.common.LightUser.Getter,
    system: ActorSystem
) {

  private val IncomingUrl = config getString "incoming.url"
  private val IncomingDefaultChannel = config getString "incoming.default_channel"
  private val NetDomain = config getString "domain"

  private val isProd = NetDomain == "lidraughts.org"

  lazy val api = new SlackApi(client, isProd, getLightUser)

  private lazy val client = new SlackClient(
    url = IncomingUrl,
    defaultChannel = IncomingDefaultChannel
  )

  system.lidraughtsBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case d: ChargeEvent => api charge d
      case DeployPre => api.deployPre
      case DeployPost => api.deployPost
      case Note(from, to, text, true) if from != "Irwin" => api.userModNote(from, to, text)
      case e: Event => api publishEvent e
    }
  })), 'deploy, 'slack, 'plan, 'userNote)
}

object Env {

  lazy val current: Env = "slack" boot new Env(
    system = lidraughts.common.PlayApp.system,
    getLightUser = lidraughts.user.Env.current.lightUser,
    config = lidraughts.common.PlayApp loadConfig "slack"
  )
}
