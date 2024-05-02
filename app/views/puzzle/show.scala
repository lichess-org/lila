package views.puzzle

import play.api.libs.json.{ JsObject, Json }

import lila.app.UiEnv.{ *, given }

import lila.common.Json.given

object show:

  def apply(
      puzzle: lila.puzzle.Puzzle,
      data: JsObject,
      pref: JsObject,
      settings: lila.puzzle.PuzzleSettings,
      langPath: Option[lila.ui.LangPath] = None
  )(using ctx: Context) =
    val isStreak = data.value.contains("streak")
    Page(if isStreak then "Puzzle Streak" else trans.site.puzzles.txt())
      .cssTag("puzzle")
      .cssTag(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .cssTag(ctx.pref.hasVoice.option("voice"))
      .cssTag(ctx.blind.option("round.nvui"))
      .js(puzzleNvuiTag)
      .js(
        PageModule(
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
      )
      .csp(views.analyse.ui.csp)
      .graph(
        OpenGraph(
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
      )
      .hrefLangs(langPath)
      .zoom
      .zen:
        bits.show.preload
