package views
package html.puzzle

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.puzzle.{ Puzzle, PuzzleAngle, PuzzleTheme }

object theme {

  def list(all: PuzzleAngle.All)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Puzzle themes",
      moreCss = cssTag("puzzle.page")
    )(
      main(cls := "page-menu")(
        bits.pageMenu("themes"),
        div(cls := "page-menu__content box")(
          h1(trans.puzzle.puzzleThemes()),
          div(cls := "puzzle-themes")(
            h2(id := "openings")("By game opening"),
            div(cls := "puzzle-themes__list")(all.openings map { case (opening, count) =>
              a(cls := "puzzle-themes__link", href := routes.Puzzle.show(opening.key.value))(
                span(
                  h3(
                    opening.name,
                    em(count.localize)
                  )
                )
              )
            }),
            all.themes map { case (cat, themes) =>
              frag(
                h2(id := cat.key)(cat()),
                div(cls := s"puzzle-themes__list ${cat.key.replace(":", "-")}")(
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
                        h3(trans.puzzleTheme.playerGames()),
                        span(trans.puzzleTheme.playerGamesDescription())
                      )
                    )
                )
              )
            },
            p(cls := "puzzle-themes__db text", dataIcon := "")(
              trans.puzzleTheme.puzzleDownloadInformation(
                a(href := "https://database.lichess.org/")("database.lichess.org")
              )
            )
          )
        )
      )
    )
}
