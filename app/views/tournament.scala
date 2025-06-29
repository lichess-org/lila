package views.tournament

import lila.app.UiEnv.{ *, given }
import lila.tournament.ui.*

lazy val ui = TournamentUi(helpers)(env.tournament.getTourName)

lazy val teamBattle = TeamBattleUi(helpers)

lazy val list = TournamentList(helpers, ui)(
  communityMenu = views.user.bits.communityMenu("tournament"),
  shieldMenu = views.user.bits.communityMenu("shield")
)

lazy val moderation = ModerationUi(helpers, ui)

private lazy val showUi = TournamentShow(helpers, views.gathering)(
  variantTeamLinks = lila.team.Team.variants.view
    .mapValues: team =>
      (team, teamLink(team, true))
    .toMap
)
export showUi.faq.page as faq

lazy val form = TournamentForm(helpers, showUi)(
  modMenu = lila.ui.bits.modMenu("tour"),
  views.setup.translatedVariantChoicesWithVariantsById
)

def show(
    tour: lila.tournament.Tournament,
    verdicts: lila.gathering.Condition.WithVerdicts,
    data: play.api.libs.json.JsObject,
    chatOption: Option[lila.chat.UserChat.Mine],
    streamers: List[UserId],
    shieldOwner: Option[UserId]
)(using ctx: Context) =
  val chat = chatOption.map: c =>
    views.chat.frag -> views.chat.json(
      c.chat,
      c.lines,
      name = trans.site.chatRoom.txt(),
      timeout = c.timeout,
      public = true,
      resourceId = lila.chat.Chat.ResourceId(s"tournament/${c.chat.id}"),
      localMod = ctx.userId.has(tour.createdBy),
      writeable = !c.locked
    )
  showUi(tour, verdicts, shieldOwner, data, chat, views.streamer.bits.contextual(streamers))
