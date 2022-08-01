package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.PopularOpenings
import lila.opening.OpeningData
import lila.common.LilaOpeningFamily
import lila.common.LilaOpening

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
          openings.treeByMove.map { case (first, sub) =>
            frag(
              h2(s"1. $first"),
              sub.map { case (fam, ops) =>
                familyWithOpenings(fam, ops)
              }
            )
          },
          h2("Others"),
          div(cls := "opening__index__others")(
            openings.treeOthers map { case (fam, ops) =>
              familyWithOpenings(fam, ops)
            }
          )
        )
      )
    }

  private def familyWithOpenings(fam: LilaOpeningFamily, ops: List[OpeningData])(implicit ctx: Context) =
    div(cls := "opening__index__sub-tree")(
      h3(a(href := routes.Opening.show(fam.key.value))(fam.name.value)),
      div(cls := "opening__index__links")(
        ops.filter(_.opening.variation != LilaOpening.otherVariations) map openingLink
      )
    )

  private def openingLink(op: OpeningData)(implicit ctx: Context) =
    span(
      a(href := routes.Opening.show(op.key.value))(op.opening.variation.name)
    ) // , " ", op.nbGames.localize)
}
