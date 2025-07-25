package views

import play.api.libs.json.*

import lila.app.UiEnv.{ *, given }
import lila.chat.UserChat

object simul:

  val ui = lila.simul.ui.SimulUi(helpers)
  val home = lila.simul.ui.SimulHome(helpers, ui)
  val form = lila.simul.ui.SimulFormUi(helpers)(
    views.setup.setupCheckboxes,
    views.setup.translatedVariantChoicesWithVariantsById
  )
  val showUi = lila.simul.ui.SimulShow(helpers, views.gathering)

  def show(
      sim: lila.simul.Simul,
      socket: lila.core.socket.SocketVersion,
      data: JsObject,
      chat: Option[UserChat.Mine],
      stream: Option[lila.streamer.Stream],
      verdicts: lila.gathering.Condition.WithVerdicts
  )(using ctx: Context): Page =
    val streamFrag = stream.map: s =>
      views.streamer.bits.contextual(s.streamer.userId)
    showUi(sim, socket, data, renderChat(chat, ctx.is(sim.hostId)), streamFrag, verdicts)

  private def renderChat(c: Option[UserChat.Mine], userIsHost: Boolean)(using
      Context
  ): Option[(JsObject, Frag)] =
    c.map: c =>
      views.chat.json(
        c.chat,
        c.lines,
        name = trans.site.chatRoom.txt(),
        timeout = c.timeout,
        public = true,
        resourceId = lila.chat.Chat.ResourceId(s"simul/${c.chat.id}"),
        localMod = userIsHost
      ) -> views.chat.frag
