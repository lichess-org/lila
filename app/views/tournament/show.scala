package views.html
package tournament

import play.api.libs.json.Json

import lila.common.Json.given
import lila.app.templating.Environment.{ *, given }
import lila.tournament.Tournament

object show:

  lazy val ui = lila.tournament.ui.TournamentShow(helpers, views.html.gathering)(
    variantTeamLinks = lila.team.Team.variants.view
      .mapValues: team =>
        (team, teamLink(team, true))
      .toMap
  )

  def apply(
      tour: Tournament,
      verdicts: lila.gathering.Condition.WithVerdicts,
      data: play.api.libs.json.JsObject,
      chatOption: Option[lila.chat.UserChat.Mine],
      streamers: List[UserId],
      shieldOwner: Option[UserId]
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = s"${tour.name()} #${tour.id}",
      pageModule = PageModule(
        "tournament",
        Json.obj(
          "data"   -> data,
          "i18n"   -> views.html.tournament.ui.jsI18n(tour),
          "userId" -> ctx.userId,
          "chat" -> chatOption.map: c =>
            chat.json(
              c.chat,
              c.lines,
              name = trans.site.chatRoom.txt(),
              timeout = c.timeout,
              public = true,
              resourceId = lila.chat.Chat.ResourceId(s"tournament/${c.chat.id}"),
              localMod = ctx.userId.has(tour.createdBy),
              writeable = !c.locked
            ),
          "showRatings" -> ctx.pref.showRatings
        )
      ).some,
      moreCss = cssTag:
        if tour.isTeamBattle then "tournament.show.team-battle"
        else "tournament.show"
      ,
      openGraph = lila.web
        .OpenGraph(
          title = s"${tour.name()}: ${tour.variant.name} ${tour.clock.show} ${tour.mode.name} #${tour.id}",
          url = s"$netBaseUrl${routes.Tournament.show(tour.id).url}",
          description =
            s"${tour.nbPlayers} players compete in the ${showEnglishDate(tour.startsAt)} ${tour.name()}. " +
              s"${tour.clock.show} ${tour.mode.name} games are played during ${tour.minutes} minutes. " +
              tour.winnerId.fold("Winner is not yet decided."): winnerId =>
                s"${titleNameOrId(winnerId)} takes the prize home!"
        )
        .some,
      csp = defaultCsp.withLilaHttp.some
    ):
      ui.show(
        tour,
        verdicts,
        shieldOwner,
        chat = chatOption.isDefined.option(views.html.chat.frag),
        streamers = views.html.streamer.bits.contextual(streamers)
      )
