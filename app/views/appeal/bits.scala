package views.html
package appeal

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*

object bits:

  def layout(title: String)(body: Frag)(using PageContext) =
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
