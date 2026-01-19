package views.swiss

import lila.app.UiEnv.{ *, given }

lazy val ui = lila.swiss.ui.SwissBitsUi(helpers, env.swiss.getName)
lazy val home = lila.swiss.ui.SwissHomeUi(helpers)
lazy val form = lila.swiss.ui.SwissFormUi(helpers)(views.setup.translatedVariantChoicesWithVariants)
lazy val showUi = lila.swiss.ui.SwissShowUi(helpers, ui, views.gathering)

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
  val chatData = chat.map: c =>
    views.chat.json(
      c.chat,
      c.lines,
      name = trans.site.chatRoom.txt(),
      timeout = c.timeout,
      public = true,
      resource = lila.core.chat.PublicSource.Swiss(s.id),
      localMod = isLocalMod,
      writeable = !c.locked
    ) -> views.chat.frag
  showUi(s, team, verdicts, data, chatData, streamersFrag)
