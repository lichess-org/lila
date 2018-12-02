package views
package html.site

import scalatags.Text.all._

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._

object variant {

  def show(
    doc: io.prismic.Document,
    resolver: io.prismic.DocumentLinkResolver,
    variant: chess.variant.Variant,
    perfType: lila.rating.PerfType
  )(implicit ctx: Context) = layout(active = perfType.some, title = s"${variant.name} • ${variant.title}") {
    div(cls := "content_box small_box doc_box")(
      h1(cls := "lichess_title text", dataIcon := perfType.iconChar)(variant.name),
      h2(cls := "headline")(variant.title),
      div(cls := "body")(raw(~doc.getHtml("variant.content", resolver)))
    )
  }

  def home(
    doc: io.prismic.Document,
    resolver: io.prismic.DocumentLinkResolver
  )(implicit ctx: Context) = layout(title = "Lichess variants") {
    div(cls := "content_box small_box doc_box no_padding")(
      h1(cls := "lichess_title text", dataIcon := "")("Lichess variants"),
      div(cls := "body content_box_content")(raw(~doc.getHtml("doc.content", resolver))),
      div(cls := "variants")(
        lila.rating.PerfType.variants map { pt =>
          val variant = lila.rating.PerfType variantOf pt
          a(cls := "variant text", href := routes.Page.variant(pt.key), dataIcon := pt.iconChar)(
            h2(variant.name),
            h3(cls := "headline")(variant.title)
          )
        }
      )
    )
  }

  private def layout(
    title: String,
    active: Option[lila.rating.PerfType] = None,
    openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    menu = Some(frag(
      lila.rating.PerfType.variants map { pt =>
        a(
          cls := Map("text" -> true, "active" -> active.has(pt)),
          href := routes.Page.variant(pt.key),
          dataIcon := pt.iconChar
        )(pt.name)
      }
    )),
    moreCss = cssTags("page.css", "variant.css"),
    openGraph = openGraph
  )(body)
}
