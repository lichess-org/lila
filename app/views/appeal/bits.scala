package views.html
package appeal

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object bits {

  def layout(title: String)(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = frag(
        cssTag("form3"),
        cssTag("appeal"),
        isGranted(_.UserModView) option cssTag("mod.user")
      ),
      moreJs = frag(
        jsModule("appeal"),
        isGranted(_.UserModView) option jsModule("mod.user")
      )
    )(body)
}
