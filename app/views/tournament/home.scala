package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.tournament.Tournament

import controllers.routes

object home {

  def apply(
    scheduled: List[Tournament],
    finished: lila.common.paginator.Paginator[Tournament],
    winners: lila.tournament.AllWinners,
    json: play.api.libs.json.JsObject
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.tournaments.txt(),
      moreCss = responsiveCssTag("tournament.home"),
      wrapClass = "full-screen-force",
      responsive = true,
      moreJs = frag(
        infiniteScrollTag,
        jsAt(s"compiled/lichess.tournamentSchedule${isProd ?? (".min")}.js"),
        embedJs(s"""var app=LichessTournamentSchedule.app(document.querySelector('.tour__schedule__chart'), {
data: ${safeJsonValue(json)},
i18n: ${jsI18n()}
});
var d=lichess.StrongSocket.defaults;d.params.flag="tournament";d.events.reload=app.update;""")
      ),
      openGraph = lila.app.ui.OpenGraph(
        url = s"$netBaseUrl${routes.Tournament.home().url}",
        title = trans.tournamentHomeTitle.txt(),
        description = trans.tournamentHomeDescription.txt()
      ).some
    ) {
        main(cls := "tour-home")(
          st.aside(cls := "tour-home__side")(
            p(
              a(href := "/tournament/calendar")(trans.tournamentCalendar()), br,
              a(href := routes.Tournament.help("arena".some))(trans.tournamentFAQ.frag())
            ),
            h2(
              a(href := routes.Tournament.leaderboard)(trans.leaderboard.frag())
            ),
            ul(cls := "leaderboard")(
              winners.top.map { w =>
                li(
                  userIdLink(w.userId.some),
                  a(title := w.tourName, href := routes.Tournament.show(w.tourId))(scheduledTournamentNameShortHtml(w.tourName))
                )
              }
            ),
            h2(trans.lichessTournaments.frag()),
            div(cls := "scheduled")(
              scheduled.map { tour =>
                tour.schedule.filter(s => s.freq != lila.tournament.Schedule.Freq.Hourly) map { s =>
                  a(href := routes.Tournament.show(tour.id), dataIcon := tournamentIconChar(tour))(
                    strong(tour.name),
                    momentFromNow(s.at)
                  )
                }
              }
            )
          ),
          st.section(cls := "tour__schedule box")(
            div(cls := "box__top")(
              h1(trans.tournaments()),
              ctx.isAuth option div(cls := "box__top__actions")(a(
                href := routes.Tournament.form(),
                cls := "button",
                title := trans.createANewTournament.txt()
              )("+"))
            ),
            div(cls := "tour__schedule__chart")
          ),
          div(id := "tournament_list", "tour__list")(
            table(cls := "slist finished")(
              thead(
                tr(
                  th(colspan := 2, cls := "large")(trans.finished()),
                  th(trans.duration()),
                  th(trans.winner()),
                  th(trans.players())
                )
              ),
              finishedPaginator(finished)
            )
          )
        )
      }
}
