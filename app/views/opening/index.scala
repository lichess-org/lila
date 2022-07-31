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
      main(cls := "page box box-pad opening__list")(
        h1("Chess openings"),
        div(
          coll.treeList.map { case (first, subTree) =>
            frag(
              h2(s"1. $first"),
              div(
                subTree.map { case (second, openings) =>
                  div(cls := "opening__list__sub-tree")(
                    h3(s"... $second"),
                    div(cls := "opening__list__links")(
                      openings map { fam =>
                        h4(
                          a(href := routes.Opening.family(fam.fam.key.value))(fam.fam.name.value)
                        )
                      }
                    )
                  )
                }
              )
            )
          }
        )
      )
    }
}
