package lila.irc

import com.softwaremill.macwire._
import play.api.{ Configuration, Mode }
import play.api.libs.ws.StandaloneWSClient

import lila.common.Lilakka
import lila.common.config._
import lila.hub.actorApi.plan.ChargeEvent
import lila.hub.actorApi.slack.Event
import lila.hub.actorApi.user.Note

@Module
final class Env(
    appConfig: Configuration,
    getLightUser: lila.common.LightUser.Getter,
    noteApi: lila.user.NoteApi,
    ws: StandaloneWSClient,
    shutdown: akka.actor.CoordinatedShutdown,
    mode: Mode
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val incomingUrl = appConfig.get[Secret]("slack.incoming.url")

  private lazy val client = wire[SlackClient]

  lazy val api: SlackApi = wire[SlackApi]

  if (mode == Mode.Prod) {
    api.publishInfo("Lichess has started!")
    Lilakka.shutdown(shutdown, _.PhaseBeforeServiceUnbind, "Tell slack")(api.stop _)
  }

  lila.common.Bus.subscribeFun("slack", "plan", "userNote") {
    case d: ChargeEvent                                => api.charge(d).unit
    case Note(from, to, text, true) if from != "Irwin" => api.userModNote(from, to, text).unit
    case e: Event                                      => api.publishEvent(e).unit
  }
}
