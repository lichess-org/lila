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

  private lazy val slackClient = new SlackClient(ws, appConfig.get[Secret]("slack.incoming.url"))

  private lazy val discordClient = new DiscordClient(ws, appConfig.get[Secret]("discord.webhook.url"))

  private val zulipConfig      = appConfig.get[ZulipClient.Config]("zulip")(ZulipClient.zulipConfigLoader)
  private lazy val zulipClient = wire[ZulipClient]

  lazy val slack: SlackApi = wire[SlackApi]

  lazy val api: IrcApi = wire[IrcApi]

  if (mode == Mode.Prod) {
    slack.publishInfo("Lichess has started!")
    Lilakka.shutdown(shutdown, _.PhaseBeforeServiceUnbind, "Tell slack")(slack.stop _)
  }

  lila.common.Bus.subscribeFun("slack", "plan", "userNote") {
    case d: ChargeEvent                                => slack.charge(d).unit
    case Note(from, to, text, true) if from != "Irwin" => api.userModNote(from, to, text).unit
    case e: Event                                      => slack.publishEvent(e).unit
  }
}
