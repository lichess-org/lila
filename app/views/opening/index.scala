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
              sub.map { case (fam, nbGames, ops) =>
                familyWithOpenings(fam, nbGames, ops)
              }
            )
          },
          h2("Others"),
          div(cls := "opening__index__others")(
            openings.treeOthers map { case (fam, nbGames, ops) =>
              familyWithOpenings(fam, nbGames, ops)
            }
          )
        )
      )
    }

  private def familyWithOpenings(fam: LilaOpeningFamily, nbGames: Int, ops: List[OpeningData])(implicit
      ctx: Context
  ) =
    div(cls := "opening__index__sub-tree")(
      h3(a(href := routes.Opening.show(fam.key.value))(fam.name.value), em(nbGames.localize)),
      div(cls := "opening__index__links")(
        ops map { op =>
          span(openingLink(op), em(op.nbGames.localize))
        }
      )
    )

  private[opening] def openingLink(op: OpeningData.Base)(implicit ctx: Context) =
    a(href := routes.Opening.show(op.key.value))(op.opening.variation.name)
}
