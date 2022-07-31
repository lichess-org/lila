package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.OpeningFamilyData

object index {

  def apply(fams: List[OpeningFamilyData])(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      title = s"${trans.opening.txt()}"
    ) {
      main(cls := "page box box-pad")(
        h1("Chess openings"),
        div(
          fams.map { fam =>
            h2(
              a(href := routes.Opening.family(fam.fam.key.value))(
                fam.fam.name.value,
                " ",
                fam.nbGames
              )
            )
          }
        )
      )
    }
}
