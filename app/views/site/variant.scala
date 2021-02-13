package views
package html.site

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object variant {

  def show(
      doc: io.prismic.Document,
      resolver: io.prismic.DocumentLinkResolver,
      variant: chess.variant.Variant,
      perfType: lila.rating.PerfType
  )(implicit ctx: Context) =
    layout(
      active = perfType.some,
      title = s"${variant.name} • ${variant.title}",
      klass = "box-pad page variant"
    )(
      h1(cls := "text", dataIcon := perfType.iconChar)(variant.name),
      h2(cls := "headline")(variant.title),
      div(cls := "body")(raw(~doc.getHtml("variant.content", resolver)))
    )

  def home(
      doc: io.prismic.Document,
      resolver: io.prismic.DocumentLinkResolver
  )(implicit ctx: Context) =
    layout(
      title = "Lichess variants",
      klass = "variants"
    )(
      h1("Lichess variants"),
      div(cls := "body box__pad")(raw(~doc.getHtml("doc.content", resolver))),
      div(cls := "variants")(
        lila.rating.PerfType.variants map { pt =>
          val variant = lila.rating.PerfType variantOf pt
          a(cls := "variant text box__pad", href := routes.Page.variant(pt.key), dataIcon := pt.iconChar)(
            span(
              h2(variant.name),
              h3(cls := "headline")(variant.title)
            )
          )
        }
      )
    )

  private def layout(
      title: String,
      klass: String,
      active: Option[lila.rating.PerfType] = None,
      openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("variant"),
      openGraph = openGraph
    )(
      main(cls := "page-menu")(
        st.aside(cls := "page-menu__menu subnav")(
          lila.rating.PerfType.variants map { pt =>
            a(
              cls := List("text" -> true, "active" -> active.has(pt)),
              href := routes.Page.variant(pt.key),
              dataIcon := pt.iconChar
            )(pt.trans)
          }
        ),
        div(cls := s"page-menu__content box $klass")(body)
      )
    )
}
