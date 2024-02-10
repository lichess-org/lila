package views
package html.site

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object variant:

  def show(
      p: lila.cms.CmsPage.Render,
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
      div(cls := "body expand-text")(
        views.html.cms.editButton(p),
        rawHtml(lila.blog.Youtube.augmentEmbeds(p.html))
      )
    )

  def home(using PageContext) =
    layout(
      title = "Lichess variants",
      klass = "variants"
    )(
      h1(cls := "box__top")("Lichess variants"),
      div(cls := "body box__pad")(
        "Chess variants introduce variations of or new mechanics in regular Chess that gives it a unique, compelling, or sophisticated gameplay. Are you ready to think outside the box?"
      ),
      div(cls := "variants")(
        lila.rating.PerfType.variants map: pt =>
          val variant = lila.rating.PerfType variantOf pt
          a(cls := "variant text box__pad", href := routes.Cms.variant(pt.key), dataIcon := pt.icon):
            span(
              h2(variant.name),
              h3(cls := "headline")(variant.title)
            )
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
      moreJs = jsModule("expandText"),
      openGraph = openGraph
    ):
      main(cls := "page-menu")(
        views.html.site.bits.pageMenuSubnav(
          lila.rating.PerfType.variants map { pt =>
            a(
              cls      := List("text" -> true, "active" -> active.has(pt)),
              href     := routes.Cms.variant(pt.key),
              dataIcon := pt.icon
            )(pt.trans)
          }
        ),
        div(cls := s"page-menu__content box $klass")(body)
      )
