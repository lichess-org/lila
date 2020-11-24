package views
package html.puzzle

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object theme {

  def list(implicit ctx: Context) =
    views.html.base.layout(
      title = "Puzzle themes",
      moreCss = cssTag("puzzle.page")
    )(
      main(cls := "page-small box box-pad")(
        h1("Puzzle themes"),
        div(cls := "puzzle-themes")(
          lila.puzzle.PuzzleTag.sorted map { pt =>
            a(cls := "box__pad", href := routes.Puzzle.home())(
              span(
                h2(pt.trans()),
                h3(cls := "headline")("Description")
              )
            )
          }
        )
      )
    )
}
