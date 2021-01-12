package views
package html.puzzle

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
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
      subtitle = "Let's see how good you've been doing",
      dashOpt = dashOpt,
      moreJs = dashOpt ?? { dash =>
        val mostPlayed = dash.mostPlayed.sortBy { case (key, _) =>
          PuzzleTheme(key).name.txt()
        }
        frag(
          jsModule("puzzle.dashboard"),
          embedJsUnsafeLoadThen(s"""LichessPuzzleDashboard.renderRadar(${safeJsonValue(
            Json
              .obj(
                "radar" -> Json.obj(
                  "labels" -> mostPlayed.map { case (key, _) =>
                    PuzzleTheme(key).name.txt()
                  },
                  "datasets" -> Json.arr(
                    Json.obj(
                      "label" -> "Performance",
                      "data" -> mostPlayed.map { case (_, results) =>
                        results.performance
                      }
                    )
                  )
                )
              )
          )})""")
        )
      }
    ) { dash =>
      frag(
        div(cls := s"${baseClass}__global")(
          metricsOf(days, PuzzleTheme.mix.key, dash.global),
          canvas(cls := s"${baseClass}__radar")
        )
      )
    }

// data: {
//     labels: ['Running', 'Swimming', 'Eating', 'Cycling'],
//     datasets: [{
//         data: [20, 10, 4, 2]
//     }]
// }

  def improvementAreas(user: User, dashOpt: Option[PuzzleDashboard], days: Int)(implicit ctx: Context) =
    dashboardLayout(
      user = user,
      days = days,
      "improvementAreas",
      title =
        if (ctx is user) trans.puzzle.improvementAreas.txt()
        else s"${user.username} improvement areas",
      subtitle = "Train these to optimize your progress!",
      dashOpt = dashOpt
    ) { dash =>
      themeSelection(days, dash.weakThemes)
    }

  def strengths(user: User, dashOpt: Option[PuzzleDashboard], days: Int)(implicit ctx: Context) =
    dashboardLayout(
      user = user,
      days = days,
      "strengths",
      title =
        if (ctx is user) trans.puzzle.strengths.txt()
        else s"${user.username} puzzle strengths",
      subtitle = "You perform the best in these themes",
      dashOpt = dashOpt
    ) { dash =>
      themeSelection(days, dash.strongThemes)
    }

  private def dashboardLayout(
      user: User,
      days: Int,
      path: String,
      title: String,
      subtitle: String,
      dashOpt: Option[PuzzleDashboard],
      moreJs: Frag = emptyFrag
  )(
      body: PuzzleDashboard => Frag
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("puzzle.dashboard"),
      moreJs = moreJs
    )(
      main(cls := "page-menu")(
        bits.pageMenu("dashboard"),
        div(cls := s"page-menu__content box box-pad $baseClass")(
          div(cls := "box__top")(
            iconTag('-'),
            h1(
              title,
              strong(subtitle)
            ),
            views.html.base.bits.mselect(
              s"${baseClass}__day-select box__top__actions",
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
