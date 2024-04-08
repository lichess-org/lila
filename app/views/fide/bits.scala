package views.html.fide
import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.*
import lila.core.fide.FideTC

private object bits:

  def layout(title: String, active: String)(modifiers: Modifier*)(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("fide"),
      title = title,
      moreJs = frag(infiniteScrollTag)
    ):
      main(cls := "page-menu")(
        views.html.relay.tour.pageMenu(active),
        div(cls := "page-menu__content box")(modifiers)
      )

  val tcTrans: List[(FideTC, lila.core.i18n.I18nKey)] =
    List(
      FideTC.standard -> trans.site.classical,
      FideTC.rapid    -> trans.site.rapid,
      FideTC.blitz    -> trans.site.blitz
    )
