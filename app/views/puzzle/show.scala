package views.html.puzzle

import controllers.routes
import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.Json.given
import lila.common.String.html.safeJsonValue

object show:

  def apply(
      puzzle: lila.puzzle.Puzzle,
      data: JsObject,
      pref: JsObject,
      settings: lila.puzzle.PuzzleSettings,
      langPath: Option[lila.common.LangPath] = None
  )(using ctx: PageContext) =
    val isStreak = data.value.contains("streak")
    views.html.base.layout(
      title = if isStreak then "Puzzle Streak" else trans.puzzles.txt(),
      moreCss = frag(
        cssTag("puzzle"),
        ctx.pref.hasKeyboardMove option cssTag("keyboardMove"),
        ctx.pref.hasVoice option cssTag("voice"),
        ctx.blind option cssTag("round.nvui")
      ),
      moreJs = frag(
        puzzleNvuiTag,
        jsModuleInit(
          "puzzle",
          Json
            .obj(
              "data"        -> data,
              "pref"        -> pref,
              "i18n"        -> bits.jsI18n(streak = isStreak),
              "showRatings" -> ctx.pref.showRatings,
              "settings" -> Json.obj("difficulty" -> settings.difficulty.key).add("color" -> settings.color)
            )
            .add("themes" -> ctx.isAuth.option(bits.jsonThemes))
        )
      ),
      csp = analysisCsp.some,
      openGraph = lila.app.ui
        .OpenGraph(
          image = cdnUrl(
            routes.Export.puzzleThumbnail(puzzle.id, ctx.pref.theme.some, ctx.pref.pieceSet.some).url
          ).some,
          title =
            if isStreak then "Puzzle Streak"
            else s"Chess tactic #${puzzle.id} - ${puzzle.color.name.capitalize} to play",
          url = s"$netBaseUrl${routes.Puzzle.show(puzzle.id).url}",
          description =
            if isStreak then trans.puzzle.streakDescription.txt()
            else
              val findMove = puzzle.color.fold(
                trans.puzzle.findTheBestMoveForWhite,
                trans.puzzle.findTheBestMoveForBlack
              )
              s"Lichess tactic trainer: ${findMove.txt()}. Played by ${puzzle.plays} players."
        )
        .some,
      zoomable = true,
      zenable = true,
      withHrefLangs = langPath
    ) {
      main(cls := "puzzle")(
        st.aside(cls := "puzzle__side")(
          div(cls := "puzzle__side__metas")
        ),
        div(cls := "puzzle__board main-board")(chessgroundBoard),
        div(cls := "puzzle__tools"),
        div(cls := "puzzle__controls")
      )
    }
