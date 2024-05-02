package views.swiss

import play.api.libs.json.JsObject

import lila.app.UiEnv.{ *, given }

lazy val ui     = lila.swiss.ui.SwissBitsUi(helpers, env.swiss.getName)
lazy val home   = lila.swiss.ui.SwissHomeUi(helpers)
lazy val form   = lila.swiss.ui.SwissFormUi(helpers)(views.setup.translatedVariantChoicesWithVariants)
lazy val showUi = lila.swiss.ui.SwissShow(helpers, ui, views.gathering)

def show(
    s: lila.swiss.Swiss,
    team: lila.core.team.LightTeam,
    verdicts: lila.gathering.Condition.WithVerdicts,
    data: play.api.libs.json.JsObject,
    chat: Option[lila.chat.UserChat.Mine],
    streamers: List[UserId],
    isLocalMod: Boolean
)(using ctx: Context): Page =
  val streamersFrag = views.streamer.bits.contextual(streamers)
  showUi(s, team, verdicts, data, renderChat(chat, isLocalMod), streamersFrag, isLocalMod)

private def renderChat(c: Option[lila.chat.UserChat.Mine], isLocalMod: Boolean)(using
    Context
): Option[(JsObject, Frag)] =
  c.map: c =>
    views.chat.json(
      c.chat,
      c.lines,
      name = trans.site.chatRoom.txt(),
      timeout = c.timeout,
      public = true,
      resourceId = lila.chat.Chat.ResourceId(s"swiss/${c.chat.id}"),
      localMod = isLocalMod,
      writeable = !c.locked
    ) -> views.chat.frag
