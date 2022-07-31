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
      val (byMove, others) = coll.treeList.partition(_._2.size > 3)
      main(cls := "page box box-pad opening__list")(
        h1("Chess openings"),
        div(
          byMove.map { case (first, fams) =>
            frag(
              h2(s"1. $first"),
              div(cls := "opening__list__links")(
                fams map { fam =>
                  a(href := routes.Opening.family(fam.fam.key.value))(fam.fam.name.value)
                }
              )
            )
          },
          h2("Others"),
          div(cls := "opening__list__links")(
            others.flatMap(_._2) map { fam =>
              h4(a(href := routes.Opening.family(fam.fam.key.value))(fam.fam.name.value))
            }
          )
        )
      )
    }
}
