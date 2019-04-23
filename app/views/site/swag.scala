package views.html.site

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object swag {

  def apply(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("swag"),
      title = "Lichess Swag",
      wrapClass = "full-screen-force",
      openGraph = lila.app.ui.OpenGraph(
        title = "Lichess merch store",
        description = "Great chess deserves great T-shirts! Get yourself some swag and help pay for the servers.",
        url = s"$netBaseUrl${routes.Page.swag}"
      ).some,
      csp = defaultCsp.withSpreadshirt.some
    ) {
        main(cls := "page-menu swag")(
          st.aside(cls := "page-menu__menu swag__side")(
            div(cls := "box")(
              raw(~doc.getHtml("doc.content", resolver))
            )
          ),
          div(cls := "page-menu__content box")(
            div(id := "myShop")(
              spinner,
              p(cls := "loading")(
                "Not loading? ",
                a(target := "_blank", href := "https://shop.spreadshirt.com/lichess-org/")("Open the shop in a new tab"),
                "."
              )
            ),
            embedJsUnsafe("""
    var spread_shop_config = {
      shopName: 'lichess-org',
        locale: 'us_US',
        prefix: 'https://shop.spreadshirt.com',
        baseId: 'myShop'
    };"""),
            script(src := "https://shop.spreadshirt.com/shopfiles/shopclient/shopclient.nocache.js")
          )
        )
      }
}
