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
  private val metricClass    = s"${baseClass}__metrics__metric"
  private val themeClass     = s"${baseClass}__themes__theme"
  private val dataWinPercent = attr("data-win-percent")

  def apply(dashOpt: Option[PuzzleDashboard], days: Int)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Puzzle dashboard",
      moreCss = cssTag("puzzle.dashboard")
    )(
      main(cls := s"page box box-pad $baseClass")(
        h1(
          "Puzzle dashboard",
          views.html.base.bits.mselect(
            s"${baseClass}__day-select",
            span(trans.nbDays.pluralSame(days)),
            PuzzleDashboard.dayChoices map { d =>
              a(
                cls := (d == days).option("current"),
                href := routes.Puzzle.dashboard(d)
              )(trans.nbDays.pluralSame(d))
            }
          )
        ),
        dashOpt match {
          case None =>
            div(cls := s"${baseClass}__empty")(
              a(href := routes.Puzzle.home())("Nothing to show, go play some puzzles first!")
            )
          case Some(dash) =>
            frag(
              div(cls := s"${baseClass}__global")(
                metricsOf(dash.global)
              ),
              div(cls := s"${baseClass}__themes")(
                div(cls := s"${baseClass}__themes__title")(
                  h2("Your weaknesses"),
                  p("Train these to optimize your progress!")
                ),
                themeSelection(dash weakestThemes 5)
              ),
              div(cls := s"${baseClass}__themes")(
                div(cls := s"${baseClass}__themes__title")(
                  h2("Your strengths"),
                  p("Congratulations, you did really well in these puzzles!")
                ),
                themeSelection(dash strongestThemes 5)
              )
            )
        }
      )
    )

  private def themeSelection(themes: List[(PuzzleTheme.Key, PuzzleDashboard.Results)])(implicit lang: Lang) =
    themes.map { case (key, results) =>
      div(cls := themeClass)(
        div(cls := s"${themeClass}__meta")(
          h3(cls := s"${themeClass}__name")(
            a(href := routes.Puzzle.show(key.value))(PuzzleTheme(key).name())
          ),
          p(cls := s"${themeClass}__description")(PuzzleTheme(key).description())
        ),
        metricsOf(results)
      )
    }

  private def metricsOf(results: PuzzleDashboard.Results)(implicit lang: Lang) =
    div(cls := s"${baseClass}__metrics")(
      div(cls := s"$metricClass $metricClass--played")(
        strong(results.nb.localize),
        span("played")
      ),
      div(cls := s"$metricClass $metricClass--perf")(
        strong(results.performance),
        span("performance")
      ),
      div(
        cls := s"$metricClass $metricClass--win",
        style := s"--first:${results.firstWinPercent}%;--win:${results.winPercent}%"
      )(
        strong(s"${results.winPercent}%"),
        span("solved")
      ),
      div(cls := s"$metricClass $metricClass--fix")(
        strong(results.unfixed),
        span("to replay")
      )
    )
}
