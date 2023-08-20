package views.html.analyse

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*

object bits:

  val dataPanel = attr("data-panel")

  def layout(
      title: String,
      moreCss: Frag = emptyFrag,
      moreJs: Frag = emptyFrag,
      openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Frag)(using PageContext): Frag =
    views.html.base.layout(
      title = title,
      moreCss = moreCss,
      moreJs = moreJs,
      openGraph = openGraph,
      robots = false,
      zoomable = true,
      csp = analysisCsp.withPeer.withInlineIconFont.some
    )(body)
