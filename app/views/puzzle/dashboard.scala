package views
package html.puzzle

import controllers.routes
import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.puzzle.PuzzleDashboard
import lila.puzzle.PuzzleTheme
import lila.user.User

object dashboard {

  private val baseClass      = "puzzle-dashboard"
  private val metricClass    = s"${baseClass}__metric"
  private val themeClass     = s"${baseClass}__theme"
  private val dataWinPercent = attr("data-win-percent")

  def home(user: User, dashOpt: Option[PuzzleDashboard], days: Int)(implicit ctx: Context) =
    dashboardLayout(
      user = user,
      days = days,
      path = "home",
      title =
        if (ctx is user) "Puzzle dashboard"
        else s"${user.username} puzzle dashboard",
      dashOpt = dashOpt
    ) { dash =>
      frag(
        div(cls := s"${baseClass}__global")(
          metricsOf(days, PuzzleTheme.mix.key, dash.global)
        ),
        div(cls := s"${baseClass}__themes")(
          div(cls := s"${baseClass}__themes__title")(
          ),
          themeSelection(days, dash.weakThemes)
        ),
        div(cls := s"${baseClass}__themes")(
          div(cls := s"${baseClass}__themes__title")(
            h2("Your strengths"),
            if (dash.strongThemes.size >= PuzzleDashboard.topThemesNb)
              p("Congratulations, you did really well in these puzzles!")
            else
              p("Play more puzzles to get a better analysis.")
          ),
          themeSelection(days, dash.strongThemes)
        )
      )
    }

  def weaknesses(user: User, dashOpt: Option[PuzzleDashboard], days: Int)(implicit ctx: Context) =
    dashboardLayout(
      user = user,
      days = days,
      "weaknesses",
      title =
        if (ctx is user) "My puzzle weaknesses"
        else s"${user.username} puzzle weaknesses",
      dashOpt = dashOpt
    ) { dash =>
      frag(
        p("Train these to optimize your progress!"),
        themeSelection(days, dash.weakThemes)
      )
    }

  def strengths(user: User, dashOpt: Option[PuzzleDashboard], days: Int)(implicit ctx: Context) =
    dashboardLayout(
      user = user,
      days = days,
      "strengths",
      title =
        if (ctx is user) "My puzzle strengths"
        else s"${user.username} puzzle strengths",
      dashOpt = dashOpt
    ) { dash =>
      frag(
        p("Train these to optimize your progress!"),
        themeSelection(days, dash.strongThemes)
      )
    }

  private def dashboardLayout(
      user: User,
      days: Int,
      path: String,
      title: String,
      dashOpt: Option[PuzzleDashboard]
  )(
      body: PuzzleDashboard => Frag
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("puzzle.dashboard")
    )(
      main(cls := "page-menu")(
        bits.pageMenu("dashboard"),
        div(cls := s"page-menu__content box box-pad $baseClass")(
          h1(
            title,
            views.html.base.bits.mselect(
              s"${baseClass}__day-select",
              span(trans.nbDays.pluralSame(days)),
              PuzzleDashboard.dayChoices map { d =>
                a(
                  cls := (d == days).option("current"),
                  href := s"${routes.Puzzle.dashboard(d, path)}${!(ctx is user) ?? s"?u=${user.username}"}"
                )(trans.nbDays.pluralSame(d))
              }
            )
          ),
          dashOpt match {
            case None =>
              div(cls := s"${baseClass}__empty")(
                a(href := routes.Puzzle.home())("Nothing to show, go play some puzzles first!")
              )
            case Some(dash) => body(dash)
          }
        )
      )
    )

  private def themeSelection(days: Int, themes: List[(PuzzleTheme.Key, PuzzleDashboard.Results)])(implicit
      lang: Lang
  ) =
    themes.map { case (key, results) =>
      div(cls := themeClass)(
        div(cls := s"${themeClass}__meta")(
          h3(cls := s"${themeClass}__name")(
            a(href := routes.Puzzle.show(key.value))(PuzzleTheme(key).name())
          ),
          p(cls := s"${themeClass}__description")(PuzzleTheme(key).description())
        ),
        metricsOf(days, key, results)
      )
    }

  private def metricsOf(days: Int, theme: PuzzleTheme.Key, results: PuzzleDashboard.Results)(implicit
      lang: Lang
  ) =
    div(cls := s"${baseClass}__metrics")(
      div(cls := s"$metricClass $metricClass--played")(
        strong(results.nb.localize),
        span("played")
      ),
      div(cls := s"$metricClass $metricClass--perf")(
        strong(results.performance, results.unclear ?? "?"),
        span("performance")
      ),
      div(
        cls := s"$metricClass $metricClass--win",
        style := s"--first:${results.firstWinPercent}%;--win:${results.winPercent}%"
      )(
        strong(s"${results.winPercent}%"),
        span("solved")
      ),
      a(
        cls := s"$metricClass $metricClass--fix",
        href := results.canReplay.option(routes.Puzzle.replay(days, theme.value).url)
      )(
        results.canReplay option span(cls := s"$metricClass--fix__text")(
          strong(results.unfixed),
          span("to replay")
        ),
        iconTag(if (results.canReplay) 'G' else 'E')
      )
    )
}
