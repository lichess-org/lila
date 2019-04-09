package views.html.tournament.crud

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object layout {

  def apply(title: String, evenMoreJs: Frag = emptyFrag, css: String = "mod.misc")(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = responsiveCssTag(css),
      responsive = true,
      moreJs = frag(
        flatpickrTag,
        delayFlatpickrStart,
        evenMoreJs
      )
    ) {
        main(cls := "page-menu")(
          views.html.mod.menu("tour"),
          body
        )
      }
}
