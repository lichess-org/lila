package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object topnav {

  private def linkTitle(url: String, name: Frag)(implicit ctx: Context) =
    if (ctx.blind) h3(name) else a(href := url)(name)

  def apply()(implicit ctx: Context) = st.nav(id := "topnav", cls := "hover")(
    )
}
