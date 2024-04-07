package views.html.tournament

import controllers.routes
import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.core.LangPath
import lila.tournament.Schedule.Freq
import lila.tournament.Tournament

object home:

  def apply(
      scheduled: List[Tournament],
      finished: List[Tournament],
      winners: lila.tournament.AllWinners,
      json: play.api.libs.json.JsObject
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = trans.site.tournaments.txt(),
      moreCss = cssTag("tournament.home"),
      wrapClass = "full-screen-force",
      modules = infiniteScrollTag,
      pageModule = PageModule(
        "tournament.schedule",
        Json.obj("data" -> json, "i18n" -> bits.scheduleJsI18n)
      ).some,
      openGraph = lila.app.ui
        .OpenGraph(
          url = s"$netBaseUrl${routes.Tournament.home.url}",
          title = trans.site.tournamentHomeTitle.txt(),
          description = trans.site.tournamentHomeDescription.txt()
        )
        .some,
      withHrefLangs = LangPath(routes.Tournament.home).some
    ) {
      main(cls := "tour-home")(
        st.aside(cls := "tour-home__side")(
          h2(
            a(href := routes.Tournament.leaderboard)(trans.site.leaderboard())
          ),
          ul(cls := "leaderboard")(
            winners.top.map: w =>
              li(
                userIdLink(w.userId.some),
                a(title := w.tourName, href := routes.Tournament.show(w.tourId))(
                  scheduledTournamentNameShortHtml(w.tourName)
                )
              )
          ),
          p(cls := "tour__links")(
            ctx.me.map: me =>
              frag(
                a(href := routes.UserTournament.path(me.username, "created"))(trans.arena.myTournaments()),
                br
              ),
            a(href := routes.Tournament.calendar)(trans.site.tournamentCalendar()),
            br,
            a(href := routes.Tournament.history(Freq.Unique.name))(trans.arena.history()),
            br,
            a(href := routes.Tournament.help)(trans.site.tournamentFAQ())
          ),
          h2(trans.site.lichessTournaments()),
          div(cls := "scheduled")(
            scheduled.map: tour =>
              tour.schedule.filter(s => s.freq != lila.tournament.Schedule.Freq.Hourly).map { s =>
                a(href := routes.Tournament.show(tour.id), dataIcon := tournamentIcon(tour))(
                  strong(tour.name(full = false)),
                  momentFromNow(s.at.instant)
                )
              }
          )
        ),
        st.section(cls := "tour-home__schedule box")(
          boxTop(
            h1(trans.site.tournaments()),
            ctx.isAuth.option(
              div(cls := "box__top__actions")(
                a(
                  href     := routes.Tournament.form,
                  cls      := "button button-green text",
                  dataIcon := licon.PlusButton
                )(trans.site.createANewTournament())
              )
            )
          ),
          div(cls := "tour-chart")
        ),
        div(cls := "arena-list box")(
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th(colspan := 2, cls := "large")(trans.site.finished()),
                th(cls := "date"),
                th(cls := "players")
              )
            ),
            finishedList(finished)
          )
        )
      )
    }
