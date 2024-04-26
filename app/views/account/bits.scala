package views.account

import lila.app.templating.Environment.{ *, given }

lazy val bits = lila.pref.ui.AccountUi(helpers)

def layout(
    title: String,
    active: String,
    evenMoreCss: Frag = emptyFrag,
    evenMoreJs: Frag = emptyFrag,
    modules: EsmList = Nil
)(body: Frag)(using ctx: PageContext): Frag =
  views.base.layout(
    title = title,
    moreCss = frag(cssTag("account"), evenMoreCss),
    moreJs = evenMoreJs,
    modules = jsModule("bits.account") ++ modules
  ):
    main(cls := "account page-menu")(
      bits.menu(active),
      div(cls := "page-menu__content")(body)
    )
