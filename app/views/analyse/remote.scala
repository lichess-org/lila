package views.html
package analyse

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object remote {
  def apply()(implicit ctx: Context) = views.html.base.layout(
    title = "Authorize remote engine",
    moreCss = cssTag("oauth"),
    moreJs = jsModule("analysisBoard.remote")
  ) {
    main(cls := "oauth box box-pad")(
      div(cls := "oauth__top")(
        img(
          cls := "oauth__logo",
          alt := "linked rings icon",
          src := assetUrl("images/icons/linked-rings.png")
        ),
        h1("Authorize remote engine")
      ),
      p("Do you want to use the remote engine on this device?"),
      form3.actions(
        a(href := routes.UserAnalysis.index)("Cancel"),
        button(cls := "button disabled", disabled := true, id := "engine-authorize")("Authorize"),
      ),
      div(cls := "oauth__footer")(
        p("Not owned or operated by lichess.org")
      )
    )
  }
}
