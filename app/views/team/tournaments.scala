package views.team

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.TeamInfo

// both arena and swiss
object tournaments:

  def page(t: lila.team.Team, tours: TeamInfo.PastAndNext)(using Context) =
    Page(s"${t.name} • ${trans.site.tournaments.txt()}")
      .graph(
        title = s"${t.name} team tournaments",
        url = routeUrl(routes.Team.tournaments(t.id)),
        description = shorten(t.description.unlink, 152)
      )
      .css("bits.team")
      .flag(_.fullScreen):
        main(
          div(cls := "box")(
            boxTop:
              h1(teamLink(t, true), " • ", trans.site.tournaments())
            ,
            div(cls := "team-events team-tournaments team-tournaments--both")(
              div(cls := "team-tournaments__next")(
                h2(trans.team.upcomingTournaments()),
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

  def renderList(tours: List[TeamInfo.AnyTour])(using Context) =
    tbody:
      tours.map:
        _.fold(
          views.tournament.ui.teamTournamentRow,
          views.swiss.ui.teamSwissRow
        )
