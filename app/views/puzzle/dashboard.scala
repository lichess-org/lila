package views
package html.puzzle

import controllers.routes
import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.puzzle.PuzzleDashboard
import lila.puzzle.PuzzleTheme
import lila.user.User

object dashboard:

  private val baseClass   = "puzzle-dashboard"
  private val metricClass = s"${baseClass}__metric"
  private val themeClass  = s"${baseClass}__theme"

  def home(user: User, dashOpt: Option[PuzzleDashboard], days: Int)(using ctx: PageContext) =
    dashboardLayout(
      user = user,
      days = days,
      path = "dashboard",
      title =
        if ctx is user then trans.puzzle.puzzleDashboard.txt()
        else s"${user.username} ${trans.puzzle.puzzleDashboard.txt()}",
      subtitle = trans.puzzle.puzzleDashboardDescription.txt(),
      dashOpt = dashOpt,
      moreJs = dashOpt so { dash =>
        val mostPlayed = dash.mostPlayed.sortBy { case (key, _) => PuzzleTheme(key).name.txt() }
        jsModuleInit(
          "puzzle.dashboard",
          Json.obj(
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
        )
      }
    ) { dash =>
      dash.mostPlayed.size > 2 option
        div(cls := s"${baseClass}__global")(
          metricsOf(days, PuzzleTheme.mix.key, dash.global),
          canvas(cls := s"${baseClass}__radar")
        )
    }

  def improvementAreas(user: User, dashOpt: Option[PuzzleDashboard], days: Int)(using ctx: PageContext) =
    dashboardLayout(
      user = user,
      days = days,
      "improvementAreas",
      title =
        if ctx is user then trans.puzzle.improvementAreas.txt()
        else s"${user.username} ${trans.puzzle.improvementAreas.txt()}",
      subtitle = trans.puzzle.improvementAreasDescription.txt(),
      dashOpt = dashOpt
    ) { dash =>
      dash.weakThemes.nonEmpty option themeSelection(days, dash.weakThemes)
    }

  def strengths(user: User, dashOpt: Option[PuzzleDashboard], days: Int)(using ctx: PageContext) =
    dashboardLayout(
      user = user,
      days = days,
      "strengths",
      title =
        if ctx is user then trans.puzzle.strengths.txt()
        else s"${user.username} ${trans.puzzle.strengths.txt()}",
      subtitle = trans.puzzle.strengthDescription.txt(),
      dashOpt = dashOpt
    ) { dash =>
      dash.strongThemes.nonEmpty option themeSelection(days, dash.strongThemes)
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
      body: PuzzleDashboard => Option[Frag]
  )(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("puzzle.dashboard"),
      moreJs = moreJs
    )(
      main(cls := "page-menu")(
        bits.pageMenu(path, user.some),
        div(cls := s"page-menu__content box box-pad $baseClass")(
          boxTop(
            h1(
              title,
              strong(subtitle)
            ),
            views.html.base.bits.mselect(
              s"${baseClass}__day-select box__top__actions",
              span(trans.nbDays.pluralSame(days)),
              PuzzleDashboard.dayChoices map { d =>
                a(
                  cls  := (d == days).option("current"),
                  href := routes.Puzzle.dashboard(d, path, user.username.some)
                )(trans.nbDays.pluralSame(d))
              }
            )
          ),
          dashOpt.flatMap(body) |
            div(cls := s"${baseClass}__empty")(
              a(href := routes.Puzzle.home)(trans.puzzle.noPuzzlesToShow())
            )
        )
      )
    )

  private def themeSelection(days: Int, themes: List[(PuzzleTheme.Key, PuzzleDashboard.Results)])(using
      ctx: PageContext
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

  private def metricsOf(days: Int, theme: PuzzleTheme.Key, results: PuzzleDashboard.Results)(using
      ctx: PageContext
  ) =
    div(cls := s"${baseClass}__metrics")(
      div(cls := s"$metricClass $metricClass--played")(
        trans.puzzle.nbPlayed.plural(results.nb, strong(results.nb.localize))
      ),
      ctx.pref.showRatings option div(cls := s"$metricClass $metricClass--perf")(
        strong(results.performance, results.unclear so "?"),
        span(trans.performance())
      ),
      div(
        cls   := s"$metricClass $metricClass--win",
        style := s"--first:${results.firstWinPercent}%;--win:${results.winPercent}%"
      )(
        trans.puzzle.percentSolved(strong(s"${results.winPercent}%"))
      ),
      a(
        cls  := s"$metricClass $metricClass--fix",
        href := results.canReplay.option(routes.Puzzle.replay(days, theme).url)
      )(
        results.canReplay option span(cls := s"$metricClass--fix__text")(
          trans.puzzle.nbToReplay.plural(results.unfixed, strong(results.unfixed))
        ),
        iconTag(if results.canReplay then licon.PlayTriangle else licon.Checkmark)
      )
    )
