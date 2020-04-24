package views
package html.site

import controllers.routes
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

object variant {

  def show(
    doc: io.prismic.Document,
    resolver: io.prismic.DocumentLinkResolver,
    variant: draughts.variant.Variant,
    perfType: lidraughts.rating.PerfType
  )(implicit ctx: Context) = layout(active = perfType.some, title = s"${variant.name} • ${variant.title}") {
    div(cls := "content_box small_box doc_box")(
      h1(cls := "lidraughts_title text", dataIcon := perfType.iconChar)(variant.name),
      h2(cls := "headline")(variant.title),
      div(cls := "body")(raw(~doc.getHtml("variant.content", resolver)))
    )
  }

  def home()(implicit ctx: Context) = layout(title = trans.rulesAndVariants.txt()) {
    div(cls := "content_box small_box doc_box no_padding")(
      h1(cls := "lidraughts_title text", dataIcon := "")(trans.rulesAndVariants()),
      div(cls := "body content_box_content")(
        p(trans.standardFmjdRegulationsWithDrawingRules()),
        p(trans.exploreDraughtsVariants())
      ),
      div(cls := "variants")(
        lidraughts.rating.PerfType.variantsPlus map { pt =>
          val variant = lidraughts.rating.PerfType variantOf pt
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
    active: Option[lidraughts.rating.PerfType] = None,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None
  )(body: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    menu = Some(frag(
      lidraughts.rating.PerfType.variantsPlus map { pt =>
        a(
          cls := List("text" -> true, "active" -> active.has(pt)),
          href := routes.Page.variant(pt.key),
          dataIcon := pt.iconChar
        )(pt.name)
      }
    )),
    moreCss = cssTags("page.css", "variant.css"),
    openGraph = openGraph
  )(body)
}
