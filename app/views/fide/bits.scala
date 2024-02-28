package views.html.fide

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.fide.{ FideTC, FidePlayer, Federation }

private object bits:

  def layout(title: String, active: String)(modifiers: Modifier*)(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("fide"),
      title = title,
      moreJs = frag(infiniteScrollTag)
    ):
      main(cls := "page-menu")(
        fideMenu(active),
        div(cls := "page-menu__content box")(modifiers)
      )

  private def fideMenu(active: String)(using Context) =
    views.html.site.bits.pageMenuSubnav(
      a(cls := active.active("players"), href := routes.Fide.index(1))("FIDE players"),
      a(cls := active.active("federations"), href := routes.Fide.federations(1))("FIDE federations")
    )

  val tcTrans: List[(FideTC, lila.i18n.I18nKey)] =
    List(FideTC.standard -> trans.classical, FideTC.rapid -> trans.rapid, FideTC.blitz -> trans.blitz)
