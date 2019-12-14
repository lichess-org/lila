package lila.slack

import com.softwaremill.macwire._
import play.api.Configuration
import play.api.libs.ws.WSClient

import lila.common.config._
import lila.hub.actorApi.plan.ChargeEvent
import lila.hub.actorApi.slack.Event
import lila.hub.actorApi.user.Note
import lila.hub.actorApi.{ DeployPost, DeployPre }

@Module
final class Env(
    appConfig: Configuration,
    getLightUser: lila.common.LightUser.Getter,
    mode: play.api.Mode,
    ws: WSClient
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val incomingUrl = appConfig.get[Secret]("slack.incoming.url")

  private lazy val client = wire[SlackClient]

  lazy val api: SlackApi = wire[SlackApi]

  lila.common.Bus.subscribeFun("deploy", "slack", "plan", "userNote") {
    case d: ChargeEvent                                => api charge d
    case DeployPre                                     => api.deployPre
    case DeployPost                                    => api.deployPost
    case Note(from, to, text, true) if from != "Irwin" => api.userModNote(from, to, text)
    case e: Event                                      => api publishEvent e
  }
}
