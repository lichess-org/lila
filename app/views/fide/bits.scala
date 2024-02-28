package views.html.fide

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.fide.{ FideTC, FidePlayer, Federation }

private object bits:

  def layout(title: String)(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("fide"),
      title = title,
      moreJs = frag(infiniteScrollTag)
    )

  val tcTrans: List[(FideTC, lila.i18n.I18nKey)] =
    List(FideTC.standard -> trans.classical, FideTC.rapid -> trans.rapid, FideTC.blitz -> trans.blitz)
