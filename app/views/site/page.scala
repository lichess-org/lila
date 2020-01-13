package views.html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object page {

  def apply(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("page"),
      title = s"${~doc.getText("doc.title")}"
    ) {
      main(cls := "page-small box box-pad page")(
        h1(doc.getText("doc.title")),
        div(cls := "body")(
          raw(~doc.getHtml("doc.content", resolver))
        )
      )
    }

  def ads(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) =
    help.layout(
      active = "ads",
      moreCss = cssTag("malware"),
      moreJs = jsTag("ads.js"),
      title = s"${~doc.getText("doc.title")}"
    ) {
      main(cls := "page-small box box-pad page")(
        h1(doc.getText("doc.title")),
        div(cls := "ads-vulnerable none", dataIcon := "j")(
          p(
            strong("Warning!"),
            "You are currently exposed to Internet malware."
          )
        ),
        div(cls := "ads-protected", dataIcon := "E")(
          p(
            strong("Congratulations!"),
            "You are probably safe from Internet malware."
          )
        ),
        div(cls := "body")(
          raw(~doc.getHtml("doc.content", resolver))
        )
      )
    }
}
