package views
package html.puzzle

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.puzzle.PuzzleTheme

object theme {

  def list(themes: List[(lila.i18n.I18nKey, List[PuzzleTheme.WithCount])])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Puzzle themes",
      moreCss = cssTag("puzzle.page")
    )(
      main(cls := "page-menu")(
        bits.pageMenu("themes"),
        div(cls := "page-menu__content box")(
          h1(trans.puzzle.puzzleThemes()),
          div(cls := "puzzle-themes")(
            themes map { case (cat, themes) =>
              frag(
                h2(cat()),
                div(
                  cls := List(
                    "puzzle-themes__list"     -> true,
                    cat.key.replace(":", "-") -> true
                  )
                )(
                  themes.map { pt =>
                    val url =
                      if (pt.theme == PuzzleTheme.mix) routes.Puzzle.home
                      else routes.Puzzle.show(pt.theme.key.value)
                    a(cls := "puzzle-themes__link", href := (pt.count > 0).option(url.url))(
                      span(
                        h3(
                          pt.theme.name(),
                          em(pt.count.localize)
                        ),
                        span(pt.theme.description())
                      )
                    )
                  },
                  cat.key == "puzzle:origin" option
                    a(cls := "puzzle-themes__link", href := routes.Puzzle.ofPlayer())(
                      span(
                        h3(trans.puzzleTheme.playerGames.txt()),
                        span(trans.puzzleTheme.playerGamesDescription.txt())
                      )
                    )
                )
              )
            },
            p(cls := "puzzle-themes__db text", dataIcon := "")(
              trans.puzzleTheme.puzzleDownloadInformation.txt(),
              a(href := "https://database.lichess.org/")("database.lichess.org"),
              "."
            )
          )
        )
      )
    )
}
