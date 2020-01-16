package views.html.clas

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object index {

  def apply(
      teacher: lila.clas.Teacher.WithUser
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "Lichess Classes",
      moreCss = cssTag("clas")
    ) {
      main(cls := "page box")(
        div(cls := "box__top")(
          h1("Lichess Classes")
        )
      )
    }
}
