package lila.slack

import akka.actor._
import play.api.Configuration
import play.api.libs.ws.WSClient

import lila.hub.actorApi.plan.ChargeEvent
import lila.hub.actorApi.slack.Event
import lila.hub.actorApi.user.Note
import lila.hub.actorApi.{ DeployPre, DeployPost }

final class Env(
    appConfig: Configuration,
    getLightUser: lila.common.LightUser.Getter,
    ws: WSClient,
    system: ActorSystem
) {

  private val config = appConfig.get[Configuration]("slack")
  private val IncomingUrl = config.get[String]("incoming.url")
  private val IncomingDefaultChannel = config.get[String]("incoming.default_channel")
  private val NetDomain = config.get[String]("domain")

  private val isProd = NetDomain == "lichess.org"

  lazy val api = new SlackApi(client, isProd, getLightUser)

  private lazy val client = new SlackClient(
    ws = ws,
    url = IncomingUrl,
    defaultChannel = IncomingDefaultChannel
  )

  lila.common.Bus.subscribeFun("deploy", "slack", "plan", "userNote") {
    case d: ChargeEvent => api charge d
    case DeployPre => api.deployPre
    case DeployPost => api.deployPost
    case Note(from, to, text, true) if from != "Irwin" => api.userModNote(from, to, text)
    case e: Event => api publishEvent e
  }
}
