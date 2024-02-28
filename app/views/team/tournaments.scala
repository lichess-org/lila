package views.html.team

import controllers.team.routes.{ Team as teamRoutes }
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.app.mashup.TeamInfo

import controllers.routes

object tournaments:

  def page(t: lila.team.Team, tours: TeamInfo.PastAndNext)(using PageContext) =
    views.html.base.layout(
      title = s"${t.name} • ${trans.tournaments.txt()}",
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${t.name} team tournaments",
          url = s"$netBaseUrl${teamRoutes.tournaments(t.id)}",
          description = shorten(t.description.value, 152)
        )
        .some,
      moreCss = cssTag("team"),
      wrapClass = "full-screen-force"
    ):
      main(
        div(cls := "box")(
          boxTop:
            h1(teamLink(t, true), " • ", trans.tournaments())
          ,
          div(cls := "team-events team-tournaments team-tournaments--both")(
            div(cls := "team-tournaments__next")(
              h2(trans.team.upcomingTourns()),
              table(cls := "slist slist-pad slist-invert")(
                renderList(tours.next)
              )
            ),
            div(cls := "team-tournaments__past")(
              h2(trans.team.completedTourns()),
              table(cls := "slist slist-pad")(
                renderList(tours.past)
              )
            )
          )
        )
      )

  def renderList(tours: List[TeamInfo.AnyTour])(using PageContext) =
    tbody:
      tours.map: any =>
        tr(
          cls := List(
            "enterable" -> any.isEnterable,
            "soon"      -> any.isNowOrSoon
          )
        )(
          td(cls := "icon")(iconTag(any.value.fold(tournamentIcon, _.perfType.icon))),
          td(cls := "header")(
            any.value.fold(
              t =>
                a(href := routes.Tournament.show(t.id))(
                  span(cls := "name")(t.name()),
                  span(cls := "setup")(
                    t.clock.show,
                    " • ",
                    if t.variant.exotic then t.variant.name else t.perfType.trans,
                    t.position.isDefined option frag(" • ", trans.thematic()),
                    " • ",
                    if t.mode.rated then trans.ratedTournament() else trans.casualTournament(),
                    " • ",
                    t.durationString
                  )
                ),
              s =>
                a(href := routes.Swiss.show(s.id))(
                  span(cls := "name")(s.name),
                  span(cls := "setup")(
                    s.clock.show,
                    " • ",
                    if s.variant.exotic then s.variant.name else s.perfType.trans,
                    " • ",
                    (if s.settings.rated then trans.ratedTournament else trans.casualTournament) ()
                  )
                )
            )
          ),
          td(cls := "infos")(
            any.value.fold(
              t =>
                frag(
                  t.teamBattle.fold(trans.team.innerTeam()): battle =>
                    trans.arena.nbTeamsBattle.plural(battle.teams.size, battle.teams.size.localize),
                  br,
                  renderStartsAt(any)
                ),
              s => frag(trans.swiss.xRoundsSwiss.pluralSame(s.settings.nbRounds), br, renderStartsAt(any))
            )
          ),
          td(cls := "text", dataIcon := licon.User)(any.nbPlayers.localize)
        )

  private def renderStartsAt(any: TeamInfo.AnyTour)(using Lang): Frag =
    if any.isEnterable && any.startsAt.isBeforeNow then trans.playingRightNow()
    else momentFromNowOnce(any.startsAt)
