package views.html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object page {

  def apply(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("page"),
      title = ~doc.getText("doc.title")
    ) {
      main(cls := "page-small box box-pad page")(
        h1(doc.getText("doc.title")),
        div(cls := "body")(
          raw(~doc.getHtml("doc.content", resolver))
        )
      )
    }
}
