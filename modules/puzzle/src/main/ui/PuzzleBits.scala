package lila.puzzle
package ui

import chess.format.{ BoardFen, Uci }
import play.api.libs.json.Json

import lila.ui.*

import ScalatagsTemplate.{ *, given }
import scalalib.model.Days

final class PuzzleBits(helpers: Helpers):
  import helpers.{ *, given }

  def daily(p: lila.puzzle.Puzzle, fen: BoardFen, lastMove: Uci) =
    chessgroundMini(fen, p.color, lastMove.some)(span)

  lazy val jsonThemes = PuzzleTheme.visible
    .collect { case t if t != PuzzleTheme.mix => t.key }
    .partition(PuzzleTheme.staticThemes.contains) match
    case (static, dynamic) =>
      Json.obj(
        "dynamic" -> dynamic.sorted(stringOrdering).mkString(" "),
        "static"  -> static.mkString(" ")
      )

  def pageMenu(active: String, user: Option[User], days: Days = Days(30))(using ctx: Context) =
    val u = user.filterNot(ctx.is).map(_.username)
    lila.ui.bits.pageMenuSubnav(
      a(href := routes.Puzzle.home)(
        trans.site.puzzles()
      ),
      a(cls := active.active("themes"), href := routes.Puzzle.themes)(
        trans.puzzle.puzzleThemes()
      ),
      a(cls := active.active("openings"), href := routes.Puzzle.openings())(
        trans.puzzle.byOpenings()
      ),
      a(cls := active.active("dashboard"), href := routes.Puzzle.dashboard(days, "dashboard", u))(
        trans.puzzle.puzzleDashboard()
      ),
      a(
        cls  := active.active("improvementAreas"),
        href := routes.Puzzle.dashboard(days, "improvementAreas", u)
      )(
        trans.puzzle.improvementAreas()
      ),
      a(cls := active.active("strengths"), href := routes.Puzzle.dashboard(days, "strengths", u))(
        trans.puzzle.strengths()
      ),
      a(cls := active.active("history"), href := routes.Puzzle.history(1, u))(
        trans.puzzle.history()
      ),
      a(cls := active.active("player"), href := routes.Puzzle.ofPlayer())(
        trans.puzzle.fromMyGames()
      )
    )

  def dailyLink(daily: DailyPuzzle.WithHtml)(using Translate) = a(
    href  := routes.Puzzle.daily,
    title := trans.puzzle.clickToSolve.txt()
  )(
    span(cls := "text")(trans.puzzle.puzzleOfTheDay()),
    rawHtml(daily.html),
    span(cls := "text")(daily.puzzle.color.fold(trans.site.whitePlays(), trans.site.blackPlays()))
  )

  object show:
    lazy val preload =
      main(cls := "puzzle")(
        st.aside(cls := "puzzle__side")(
          div(cls := "puzzle__side__metas")
        ),
        div(cls := "puzzle__board main-board")(chessgroundBoard),
        div(cls := "puzzle__tools"),
        div(cls := "puzzle__controls")
      )

  object dashboard:
    val baseClass   = "puzzle-dashboard"
    val metricClass = s"${baseClass}__metric"
    val themeClass  = s"${baseClass}__theme"

    def pageModule(dashOpt: Option[PuzzleDashboard])(using Translate) =
      PageModule(
        "puzzle.dashboard",
        dashOpt.so: dash =>
          val mostPlayed = dash.mostPlayed.sortBy { (key, _) => PuzzleTheme(key).name.txt() }
          Json.obj(
            "radar" -> Json.obj(
              "labels" -> mostPlayed.map: (key, _) =>
                PuzzleTheme(key).name.txt(),
              "datasets" -> Json.arr(
                Json.obj(
                  "label" -> "Performance",
                  "data" -> mostPlayed.map: (_, results) =>
                    results.performance
                )
              )
            )
          )
      ).some

    def themeSelection(days: Days, themes: List[(PuzzleTheme.Key, PuzzleDashboard.Results)])(using
        ctx: Context
    ) =
      themes.map: (key, results) =>
        div(cls := themeClass)(
          div(cls := s"${themeClass}__meta")(
            h3(cls := s"${themeClass}__name")(
              a(href := routes.Puzzle.show(key.value))(PuzzleTheme(key).name())
            ),
            p(cls := s"${themeClass}__description")(PuzzleTheme(key).description())
          ),
          metricsOf(days, key, results)
        )

    def metricsOf(days: Days, theme: PuzzleTheme.Key, results: PuzzleDashboard.Results)(using
        ctx: Context
    ) =
      div(cls := s"${baseClass}__metrics")(
        div(cls := s"$metricClass $metricClass--played")(
          trans.puzzle.nbPlayed.plural(results.nb, strong(results.nb.localize))
        ),
        ctx.pref.showRatings.option(
          div(cls := s"$metricClass $metricClass--perf")(
            strong(results.performance, results.unclear.so("?")),
            span(trans.site.performance())
          )
        ),
        div(
          cls   := s"$metricClass $metricClass--win",
          style := s"---first:${results.firstWinPercent}%;---win:${results.winPercent}%"
        )(
          trans.puzzle.percentSolved(strong(s"${results.winPercent}%"))
        ),
        a(
          cls  := s"$metricClass $metricClass--fix",
          href := results.canReplay.option(routes.Puzzle.replay(days, theme.value).url)
        )(
          results.canReplay.option(
            span(cls := s"$metricClass--fix__text")(
              trans.puzzle.nbToReplay.plural(results.unfixed, strong(results.unfixed))
            )
          ),
          iconTag(if results.canReplay then Icon.PlayTriangle else Icon.Checkmark)
        )
      )
