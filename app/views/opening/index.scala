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
                  frag(
                    second map { m => h3(s"... $m") },
                    openings map { fam =>
                      a(href := routes.Opening.family(fam.fam.key.value))(
                        fam.fam.name.value,
                        " ",
                        fam.nbGames
                      )
                    }
                  )
                }
              )
            )
          }
        )
      )
    }
}
