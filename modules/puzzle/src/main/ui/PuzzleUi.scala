package ui

import play.api.libs.json.*
import scalalib.paginator.Paginator
import lila.common.Json.given
import lila.common.LilaOpeningFamily
import lila.core.i18n.I18nKey
import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class PuzzleUi(helpers: Helpers, val bits: PuzzleBits)(
    analyseCsp: Update[ContentSecurityPolicy],
    externalEngineEndpoint: String
):
  import helpers.{ *, given }

  def show(
      puzzle: lila.puzzle.Puzzle,
      data: JsObject,
      pref: JsObject,
      settings: lila.puzzle.PuzzleSettings,
      langPath: Option[lila.ui.LangPath] = None
  )(using ctx: Context) =
    val isStreak = data.value.contains("streak")
    val pageTitle = if isStreak then "Puzzle Streak" else trans.site.puzzles.txt()
    
    Page(pageTitle)
      .css("puzzle")
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .css(ctx.pref.hasVoice.option("voice"))
      .css(ctx.blind.option("round.nvui"))
      .i18n(_.puzzle, _.puzzleTheme, _.storm)
      .js(ctx.blind.option(Esm("puzzle.nvui")))
      .js(
        PageModule(
          "puzzle",
          Json.obj(
            "data" -> data,
            "pref" -> pref,
            "showRatings" -> ctx.pref.showRatings,
            "settings" -> Json.obj("difficulty" -> settings.difficulty.key, "color" -> settings.color),
            "externalEngineEndpoint" -> externalEngineEndpoint
          ).add("themes" -> ctx.isAuth.option(bits.jsonThemes))
        )
      )
      .csp(analyseCsp)
      .graph(
        OpenGraph(
          image = cdnUrl(routes.Export.puzzleThumbnail(puzzle.id, ctx.pref.theme.some, ctx.pref.pieceSet.some).url).some,
          title = if isStreak then "Puzzle Streak" else s"Chess tactic #${puzzle.id} - ${puzzle.color.name.capitalize} to play",
          url = s"$netBaseUrl${routes.Puzzle.show(puzzle.id.value).url}",
          description = if isStreak then trans.puzzle.streakDescription.txt() else {
            val findMove = puzzle.color.fold(trans.puzzle.findTheBestMoveForWhite.txt(), trans.puzzle.findTheBestMoveForBlack.txt())
            s"Lichess tactic trainer: $findMove. Played by ${puzzle.plays} players."
          }
        )
      )
      .hrefLangs(langPath)
      .zoom
      .zen:
        bits.show.preload

  def themes(all: PuzzleAngle.All)(using ctx: Context) =
    Page(trans.puzzle.puzzleThemes.txt())
      .css("puzzle.page")
      .hrefLangs(lila.ui.LangPath(routes.Puzzle.themes)):
        main(cls := "page-menu")(
          bits.pageMenu("themes", ctx.me),
          div(cls := "page-menu__content box")(
            h1(cls := "box__top")(trans.puzzle.puzzleThemes()),
            standardFlash.map(div(cls := "box__pad")(_)),
            div(cls := "puzzle-themes")(
              all.themes.take(2).map(themeCategory),
              h2(id := "openings")(
                "By game opening",
                a(href := routes.Puzzle.openings())(trans.site.more(), " »")
              ),
              opening.listOf(all.openings.families.take(12)),
              all.themes.drop(2).map(themeCategory),
              themeInfo
            )
          )
        )

  private def themeInfo(using Context) =
    p(cls := "puzzle-themes__db text", dataIcon := Icon.Heart):
      trans.puzzleTheme.puzzleDownloadInformation:
        a(href := "https://database.lichess.org/")("database.lichess.org")

  private def themeCategory(cat: I18nKey, themes: List[PuzzleTheme.WithCount])(using
