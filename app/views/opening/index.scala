package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.PopularOpenings

object index {

  def apply(openings: PopularOpenings)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      title = s"${trans.opening.txt()}"
    ) {
      main(cls := "page box box-pad opening__index")(
        h1("Chess openings"),
        div(
          openings.treeByMove.map { case (first, ops) =>
            frag(
              h2(s"1. $first"),
              div(cls := "opening__index__links")(
                ops map { op =>
                  a(href := routes.Opening.show(op.key.value))(op.opening.name.value)
                }
              )
            )
          },
          h2("Others"),
          div(cls := "opening__index__links")(
            openings.treeOthers map { op =>
              h4(a(href := routes.Opening.show(op.key.value))(op.opening.name.value))
            }
          )
        )
      )
    }
}
