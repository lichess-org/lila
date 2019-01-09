package views.html.tournament

import play.twirl.api.Html

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
    layout(
      title = trans.tournaments.txt(),
      moreJs = frag(
        infiniteScrollTag,
        jsAt(s"compiled/lichess.tournamentSchedule${isProd ?? (".min")}.js"),
        embedJs(s"""var app=LichessTournamentSchedule.app(document.getElementById('tournament_schedule'), {
data: ${safeJsonValue(json)},
i18n: ${jsI18n()}
});
var d=lichess.StrongSocket.defaults;d.params.flag="tournament";d.events.reload=app.update;""")
      ),
      side = Some(frag(
        div(cls := "tournament_home_side")(
          div(cls := "tournament_links")(
            a(dataIcon := "", cls := "text", href := routes.Tournament.help("arena".some))(trans.tournamentFAQ())
          ),
          h2(cls := "leaderboard_title")(
            a(href := routes.Tournament.leaderboard)(trans.leaderboard())
          )
        ),
        ul(cls := "tournament_leaderboard")(
          winners.top.map { w =>
            li(
              userIdLink(w.userId.some),
              a(title := w.tourName, href := routes.Tournament.show(w.tourId))(scheduledTournamentNameShortHtml(w.tourName))
            )
          }
        ),
        h2(cls := "leaderboard_title")(trans.lichessTournaments()),
        div(cls := "scheduled_tournaments")(
          scheduled.map { tour =>
            tour.schedule.filter(s => s.freq != lila.tournament.Schedule.Freq.Hourly) map { s =>
              a(href := routes.Tournament.show(tour.id), dataIcon := tournamentIconChar(tour))(
                strong(tour.name),
                momentFromNow(s.at)
              )
            }
          }
        )
      )),
      openGraph = lila.app.ui.OpenGraph(
        url = s"$netBaseUrl${routes.Tournament.home().url}",
        title = trans.tournamentHomeTitle.txt(),
        description = trans.tournamentHomeDescription.txt()
      ).some
    ) {
        div(cls := "content_box tournament_box no_padding")(
          div(cls := "create_tournament")(
            a(href := "/tournament/calendar", cls := "blue")(trans.tournamentCalendar()),
            " ",
            ctx.isAuth option a(href := routes.Tournament.form(), cls := "button")(trans.createANewTournament())
          ),
          h1(trans.tournaments()),
          div(id := "tournament_schedule"),
          div(id := "tournament_list")(
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
