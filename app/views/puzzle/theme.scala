package views
package html.puzzle

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.i18n.I18nKey
import lila.puzzle.{ PuzzleAngle, PuzzleTheme }

object theme:

  def list(all: PuzzleAngle.All)(using ctx: Context) =
    views.html.base.layout(
      title = trans.puzzle.puzzleThemes.txt(),
      moreCss = cssTag("puzzle.page"),
      withHrefLangs = lila.common.LangPath(routes.Puzzle.themes).some
    )(
      main(cls := "page-menu")(
        bits.pageMenu("themes", ctx.me),
        div(cls := "page-menu__content box")(
          h1(cls := "box__top")(trans.puzzle.puzzleThemes()),
          standardFlash.map(div(cls := "box__pad")(_)),
          div(cls := "puzzle-themes")(
            all.themes take 2 map { case (cat, themes) =>
              themeCategory(cat, themes)
            },
            h2(id := "openings")("By game opening", a(href := routes.Puzzle.openings())(trans.more(), " »")),
            opening.listOf(all.openings.families take 12),
            all.themes drop 2 map { case (cat, themes) =>
              themeCategory(cat, themes)
            },
            info
          )
        )
      )
    )

  private[puzzle] def info(using Context) =
    p(cls := "puzzle-themes__db text", dataIcon := "")(
      trans.puzzleTheme.puzzleDownloadInformation(
        a(href := "https://database.lichess.org/")("database.lichess.org")
      )
    )

  private def themeCategory(cat: I18nKey, themes: List[PuzzleTheme.WithCount])(using Context) =
    frag(
      h2(id := cat.value)(cat()),
      div(cls := s"puzzle-themes__list ${cat.value.replace(":", "-")}")(
        themes.map { pt =>
          val url =
            if (pt.theme == PuzzleTheme.mix) routes.Puzzle.home
            else routes.Puzzle.show(pt.theme.key.value)
          a(cls := "puzzle-themes__link", href := (pt.count > 0).option(langHref(url)))(
            span(
              h3(
                pt.theme.name(),
                em(pt.count.localize)
              ),
              span(pt.theme.description())
            )
          )
        },
        cat.value == "puzzle:origin" option
          a(cls := "puzzle-themes__link", href := routes.Puzzle.ofPlayer())(
            span(
              h3(trans.puzzleTheme.playerGames()),
              span(trans.puzzleTheme.playerGamesDescription())
            )
          )
      )
    )
