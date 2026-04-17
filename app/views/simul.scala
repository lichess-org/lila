package views

import play.api.libs.json.*

import lila.app.UiEnv.{ *, given }
import lila.chat.UserChat
import lila.simul.Simul

object simul:

  val ui = lila.simul.ui.SimulUi(helpers)
  val home = lila.simul.ui.SimulHome(helpers, ui)
  val form = lila.simul.ui.SimulFormUi(helpers)(
    views.setup.setupCheckboxes,
    views.setup.translatedVariantChoicesWithVariantsById
  )
  val showUi = lila.simul.ui.SimulShow(helpers, views.gathering)

  def show(
      sim: Simul,
      socket: lila.core.socket.SocketVersion,
      data: JsObject,
      chat: Option[UserChat.Mine],
      stream: Option[lila.streamer.Stream],
      verdicts: lila.gathering.Condition.WithVerdicts
  )(using ctx: Context): Page =
    val streamFrag = stream.map: s =>
      views.streamer.bits.contextual(s.streamer.userId)
    val chatData = chat.map: c =>
      views.chat.json(
        c.chat,
        c.lines,
        name = trans.site.chatRoom.txt(),
        timeout = c.timeout,
        public = true,
        resource = lila.core.chat.PublicSource.Simul(sim.id),
        localMod = ctx.is(sim.hostId)
      ) -> views.chat.frag
    showUi(sim, socket, data, chatData, streamFrag, verdicts)
