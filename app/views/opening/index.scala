package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.FamilyDataCollection

object index {

  def apply(coll: FamilyDataCollection)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      title = s"${trans.opening.txt()}"
    ) {
      main(cls := "page box box-pad opening__index")(
        h1("Chess openings"),
        div(
          coll.treeByMove.map { case (first, fams) =>
            frag(
              h2(s"1. $first"),
              div(cls := "opening__index__links")(
                fams map { fam =>
                  a(href := routes.Opening.family(fam.fam.key.value))(fam.fam.name.value)
                }
              )
            )
          },
          h2("Others"),
          div(cls := "opening__index__links")(
            coll.treeOthers map { fam =>
              h4(a(href := routes.Opening.family(fam.fam.key.value))(fam.fam.name.value))
            }
          )
        )
      )
    }
}
