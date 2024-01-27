package views
package html.site

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object variant:

  def show(
      doc: io.prismic.Document,
      resolver: io.prismic.DocumentLinkResolver,
      variant: chess.variant.Variant,
      perfType: lila.rating.PerfType
  )(using PageContext) =
    layout(
      active = perfType.some,
      title = s"${variant.name} • ${variant.title}",
      klass = "box-pad page variant"
    )(
      boxTop(h1(cls := "text", dataIcon := perfType.icon)(variant.name)),
      h2(cls := "headline")(variant.title),
      div(cls := "body"):
        Html
          .from(doc.getHtml("variant.content", resolver))
          .map(lila.blog.Youtube.augmentEmbeds)
          .map(rawHtml)
    )

  def home(
      doc: io.prismic.Document,
      resolver: io.prismic.DocumentLinkResolver
  )(using PageContext) =
    layout(
      title = "Lichess variants",
      klass = "variants"
    )(
      h1(cls := "box__top")("Lichess variants"),
      div(cls := "body box__pad")(raw(~doc.getHtml("doc.content", resolver))),
      div(cls := "variants")(
        lila.rating.PerfType.variants map { pt =>
          val variant = lila.rating.PerfType variantOf pt
          a(cls := "variant text box__pad", href := routes.ContentPage.variant(pt.key), dataIcon := pt.icon)(
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
  )(body: Modifier*)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("variant"),
      openGraph = openGraph
    ):
      main(cls := "page-menu")(
        views.html.site.bits.pageMenuSubnav(
          lila.rating.PerfType.variants map { pt =>
            a(
              cls      := List("text" -> true, "active" -> active.has(pt)),
              href     := routes.ContentPage.variant(pt.key),
              dataIcon := pt.icon
            )(pt.trans)
          }
        ),
        div(cls := s"page-menu__content box $klass")(body)
      )
