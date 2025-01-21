package lila.irc

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import play.api.{ Configuration, Mode }

import lila.common.Lilakka
import lila.core.irc.Event
import lila.core.misc.plan.ChargeEvent

@Module
final class Env(
    appConfig: Configuration,
    noteApi: lila.core.user.NoteApi,
    ws: StandaloneWSClient,
    shutdown: akka.actor.CoordinatedShutdown,
    mode: Mode,
    getLightUser: lila.core.LightUser.Getter
)(using Executor):

  import ZulipClient.given
  private val zulipConfig      = appConfig.get[ZulipClient.Config]("zulip")
  private lazy val zulipClient = wire[ZulipClient]

  lazy val api: IrcApi = wire[IrcApi]

  if mode.isProd then
    api.publishInfo("Lichess has started!")
    Lilakka.shutdown(shutdown, _.PhaseBeforeServiceUnbind, "Tell IRC"): () =>
      api.stop()
      funit // don't wait for zulip aknowledgment to restart lila.

  lila.common.Bus.subscribeFun("slack", "plan"):
    case d: ChargeEvent => api.charge(d)
    case e: Event       => api.publishEvent(e)
