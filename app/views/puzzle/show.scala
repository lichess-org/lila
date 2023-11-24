package views.html.puzzle

import controllers.routes
import play.api.libs.json.{ JsObject, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object show {

  def apply(
      puzzle: lila.puzzle.Puzzle,
      theme: lila.puzzle.PuzzleTheme,
      data: JsObject,
      pref: JsObject,
      themeView: Boolean,
      difficulty: Option[lila.puzzle.PuzzleDifficulty] = None
  )(implicit
      ctx: Context
  ) = {
    val defaultTheme = theme == lila.puzzle.PuzzleTheme.mix
    val urlPath      = if (defaultTheme) routes.Puzzle.home.url else routes.Puzzle.show(theme.key.value).url
    views.html.base.layout(
      title = trans.puzzles.txt(),
      moreCss = cssTag("puzzle"),
      moreJs = frag(
        jsModule("puzzle", true),
        embedJsUnsafe(s"""$$(function() {
          LishogiPuzzle(${safeJsonValue(
            Json
              .obj(
                "data" -> data,
                "pref" -> pref,
                "i18n" -> bits.jsI18n
              )
              .add("themes" -> ctx.isAuth.option(bits.jsonThemes))
              .add("difficulty" -> difficulty.map(_.key))
          )})})""")
      ),
      csp = defaultCsp.withWebAssembly.some,
      shogiground = false,
      openGraph =
        if (themeView)
          lila.app.ui
            .OpenGraph(
              title = trans.puzzleDesc.txt() + (!defaultTheme ?? s" - ${theme.name.txt()}"),
              url = s"$netBaseUrl$urlPath",
              description = s"${trans.puzzleDesc.txt()}: ${theme.description.txt()}"
            )
            .some
        else
          lila.app.ui
            .OpenGraph(
              image = cdnUrl(routes.Export.puzzleThumbnail(puzzle.id.value).url).some,
              title = s"${trans.puzzleDesc.txt()} #${puzzle.id}",
              url = s"$netBaseUrl${routes.Puzzle.show(puzzle.id.value).url}",
              description = s"${trans.puzzleDesc.txt()}: " +
                transWithColorName(trans.puzzle.findTheBestMoveForX, puzzle.color, false) +
                s" ${trans.puzzle.playedXTimes.pluralSameTxt(puzzle.plays)}"
            )
            .some,
      zoomable = true,
      playing = true,
      canonicalPath = lila.common.CanonicalPath(urlPath).some,
      withHrefLangs = lila.i18n.LangList.All.some
    ) {
      main(cls := "puzzle")(
        st.aside(cls := "puzzle__side")(
          div(cls    := "puzzle__side__metas")
        ),
        div(cls := "puzzle__board main-board")(
          shogigroundEmpty(shogi.variant.Standard, puzzle.color)
        ),
        div(cls := "puzzle__tools"),
        div(cls := "puzzle__controls")
      )
    }
  }
}
