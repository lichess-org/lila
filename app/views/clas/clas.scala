package views.html.clas

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object clas {

  def index(
      classes: List[lila.clas.Clas],
      teacher: lila.clas.Teacher.WithUser
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "Lichess Classes",
      moreCss = cssTag("clas")
    ) {
      main(cls := "page box clas-index")(
        div(cls := "box__top")(
          h1("Lichess Classes"),
          a(
            href := routes.Clas.form,
            cls := "new button button-empty",
            title := "New Class",
            dataIcon := "O"
          )
        )
      )
    }

  def show(
      clas: lila.clas.Clas,
      teacher: lila.clas.Teacher.WithUser
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = clas.name,
      moreCss = cssTag("clas")
    ) {
      main(cls := "page box clas-show")(
        div(cls := "box__top")(
          h1(clas.name)
        )
      )
    }
}
