package views.puzzle

import scalalib.model.Days
import lila.app.UiEnv.{ *, given }
import lila.puzzle.{ PuzzleDashboard, PuzzleTheme }

object dashboard:

  import bits.dashboard.*

  def home(user: User, dashOpt: Option[PuzzleDashboard], days: Days)(using ctx: Context) =
    dashboardLayout(
      user = user,
      days = days,
      path = "dashboard",
      title =
        if ctx.is(user) then trans.puzzle.puzzleDashboard.txt()
        else s"${user.username} ${trans.puzzle.puzzleDashboard.txt()}",
      subtitle = trans.puzzle.puzzleDashboardDescription.txt(),
      dashOpt = dashOpt
    )(dash =>
      (dash.mostPlayed.size > 2).option(
        div(cls := s"${baseClass}__global")(
          metricsOf(days, PuzzleTheme.mix.key, dash.global),
          canvas(cls := s"${baseClass}__radar")
        )
      )
    ).js(pageModule(dashOpt))

  def improvementAreas(user: User, dashOpt: Option[PuzzleDashboard], days: Days)(using ctx: Context) =
    dashboardLayout(
      user = user,
      days = days,
      "improvementAreas",
      title =
        if ctx.is(user) then trans.puzzle.improvementAreas.txt()
        else s"${user.username} ${trans.puzzle.improvementAreas.txt()}",
      subtitle = trans.puzzle.improvementAreasDescription.txt(),
      dashOpt = dashOpt
    ): dash =>
      dash.weakThemes.nonEmpty.option(themeSelection(days, dash.weakThemes))

  def strengths(user: User, dashOpt: Option[PuzzleDashboard], days: Days)(using ctx: Context) =
    dashboardLayout(
      user = user,
      days = days,
      "strengths",
      title =
        if ctx.is(user) then trans.puzzle.strengths.txt()
        else s"${user.username} ${trans.puzzle.strengths.txt()}",
      subtitle = trans.puzzle.strengthDescription.txt(),
      dashOpt = dashOpt
    ): dash =>
      dash.strongThemes.nonEmpty.option(themeSelection(days, dash.strongThemes))

  private def dashboardLayout(
      user: User,
      days: Days,
      path: String,
      title: String,
      subtitle: String,
      dashOpt: Option[PuzzleDashboard]
  )(body: PuzzleDashboard => Option[Frag])(using Context) =
    Page(title).css("puzzle.dashboard"):
      main(cls := "page-menu")(
        bits.pageMenu(path, user.some),
        div(cls := s"page-menu__content box box-pad $baseClass")(
          boxTop(
            h1(
              title,
              strong(subtitle)
            ),
            lila.ui.bits.mselect(
              s"${baseClass}__day-select box__top__actions",
              span(trans.site.nbDays.pluralSame(days.value)),
              PuzzleDashboard.dayChoices.map: d =>
                a(
                  cls  := (d == days).option("current"),
                  href := routes.Puzzle.dashboard(d, path, user.username.some)
                )(trans.site.nbDays.pluralSame(d.value))
            )
          ),
          dashOpt.flatMap(body) |
            div(cls := s"${baseClass}__empty")(
              a(href := routes.Puzzle.home)(trans.puzzle.noPuzzlesToShow())
            )
        )
      )
