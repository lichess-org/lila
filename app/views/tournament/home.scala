package views.html.tournament

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.tournament.Schedule.Freq
import lila.tournament.Tournament

object home {

  def apply(
      scheduled: List[Tournament],
      finished: List[Tournament],
      winners: lila.tournament.AllWinners,
      json: play.api.libs.json.JsObject
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.tournaments.txt(),
      moreCss = cssTag("tournament.home"),
      wrapClass = "full-screen-force",
      moreJs = frag(
        infiniteScrollTag,
        jsModule("tournament.schedule"),
        embedJsUnsafeLoadThen(s"""LichessTournamentSchedule(${safeJsonValue(
          Json.obj(
            "data" -> json,
            "i18n" -> bits.jsI18n
          )
        )})""")
      ),
      openGraph = lila.app.ui
        .OpenGraph(
          url = s"$netBaseUrl${routes.Tournament.home.url}",
          title = trans.tournamentHomeTitle.txt(),
          description = trans.tournamentHomeDescription.txt()
        )
        .some
    ) {
      main(cls := "tour-home")(
        st.aside(cls := "tour-home__side")(
          h2(
            a(href := routes.Tournament.leaderboard)(trans.leaderboard())
          ),
          ul(cls := "leaderboard")(
            winners.top.map { w =>
              li(
                userIdLink(w.userId.some),
                a(title := w.tourName, href := routes.Tournament.show(w.tourId))(
                  scheduledTournamentNameShortHtml(w.tourName)
                )
              )
            }
          ),
          p(cls := "tour__links")(
            ctx.me map { me =>
              frag(
                a(href := routes.UserTournament.path(me.username, "created"))("My tournaments"),
                br
              )
            },
            a(href := routes.Tournament.calendar)(trans.tournamentCalendar()),
            br,
            a(href := routes.Tournament.history(Freq.Unique.name))(trans.arena.history()),
            br,
            a(href := routes.Tournament.help("arena".some))(trans.tournamentFAQ())
          ),
          h2(trans.lichessTournaments()),
          div(cls := "scheduled")(
            scheduled.map { tour =>
              tour.schedule.filter(s => s.freq != lila.tournament.Schedule.Freq.Hourly) map { s =>
                a(href := routes.Tournament.show(tour.id), dataIcon := tournamentIconChar(tour))(
                  strong(tour.name(full = false)),
                  momentFromNow(s.at)
                )
              }
            }
          )
        ),
        st.section(cls := "tour-home__schedule box")(
          div(cls := "box__top")(
            h1(trans.tournaments()),
            ctx.isAuth option div(cls := "box__top__actions")(
              a(
                href := routes.Tournament.form,
                cls := "button button-green text",
                dataIcon := "O"
              )(trans.createANewTournament())
            )
          ),
          div(cls := "tour-chart")
        ),
        div(cls := "arena-list box")(
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th(colspan := 2, cls := "large")(trans.finished()),
                th(cls := "date"),
                th(cls := "players")
              )
            ),
            finishedList(finished)
          )
        )
      )
    }
}
