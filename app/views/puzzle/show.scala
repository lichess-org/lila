package views.html.puzzle

import controllers.routes
import play.api.libs.json.{ JsObject, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.common.Json.colorWrites

object show {

  def apply(
      puzzle: lila.puzzle.Puzzle,
      data: JsObject,
      pref: JsObject,
      settings: lila.puzzle.PuzzleSettings
  )(implicit ctx: Context) = {
    val isStreak = data.value.contains("streak")
    views.html.base.layout(
      title = if (isStreak) "Puzzle Streak" else trans.puzzles.txt(),
      moreCss = frag(
        cssTag("puzzle"),
        ctx.pref.hasKeyboardMove option cssTag("keyboardMove"),
        ctx.blind option cssTag("round.nvui")
      ),
      moreJs = frag(
        puzzleTag,
        puzzleNvuiTag,
        embedJsUnsafeLoadThen(s"""LichessPuzzle(${safeJsonValue(
            Json
              .obj(
                "data"        -> data,
                "pref"        -> pref,
                "i18n"        -> bits.jsI18n(streak = isStreak),
                "showRatings" -> ctx.pref.showRatings,
                "settings" -> Json.obj("difficulty" -> settings.difficulty.key).add("color" -> settings.color)
              )
              .add("themes" -> ctx.isAuth.option(bits.jsonThemes))
          )})""")
      ),
      csp = defaultCsp.withWebAssembly.withAnyWs.some,
      chessground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          image = cdnUrl(routes.Export.puzzleThumbnail(puzzle.id.value).url).some,
          title =
            if (isStreak) "Puzzle Streak"
            else s"Chess tactic #${puzzle.id} - ${puzzle.color.name.capitalize} to play",
          url = s"$netBaseUrl${routes.Puzzle.show(puzzle.id.value).url}",
          description =
            if (isStreak) trans.puzzle.streakDescription.txt()
            else
              s"Lichess tactic trainer: ${puzzle.color
                  .fold(
                    trans.puzzle.findTheBestMoveForWhite,
                    trans.puzzle.findTheBestMoveForBlack
                  )
                  .txt()}. Played by ${puzzle.plays} players."
        )
        .some,
      zoomable = true,
      playing = true
    ) {
      main(cls := "puzzle")(
        st.aside(cls := "puzzle__side")(
          div(cls    := "puzzle__side__metas")
        ),
        div(cls := "puzzle__board main-board")(chessgroundBoard),
        div(cls := "puzzle__tools"),
        div(cls := "puzzle__controls")
      )
    }
  }
}
