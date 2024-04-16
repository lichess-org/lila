package views
package html.site

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.rating.PerfType

object variant:

  def show(
      p: lila.cms.CmsPage.Render,
      variant: chess.variant.Variant,
      perfType: PerfType
  )(using PageContext) =
    layout(
      active = perfType.key.some,
      title = s"${variant.name} • ${variant.title}",
      klass = "box-pad page variant"
    )(
      boxTop(h1(cls := "text", dataIcon := perfType.icon)(variant.name)),
      h2(cls := "headline")(variant.title),
      div(cls := "body expand-text")(views.html.cms.render(p))
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
        lila.rating.PerfType.variants.map: pk =>
          val variant = lila.rating.PerfType.variantOf(pk)
          val pt      = lila.rating.PerfType(pk)
          a(cls := "variant text box__pad", href := routes.Cms.variant(pk), dataIcon := pt.icon):
            span(
              h2(variant.name),
              h3(cls := "headline")(variant.title)
            )
      )
    )

  private def layout(
      title: String,
      klass: String,
      active: Option[PerfKey] = None,
      openGraph: Option[lila.web.OpenGraph] = None
  )(body: Modifier*)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("variant"),
      moreJs = jsModule("expandText"),
      openGraph = openGraph
    ):
      main(cls := "page-menu")(
        views.html.base.bits.pageMenuSubnav(
          lila.rating.PerfType.variants.map { pk =>
            val pt = lila.rating.PerfType(pk)
            a(
              cls      := List("text" -> true, "active" -> active.contains(pk)),
              href     := routes.Cms.variant(pk),
              dataIcon := pt.icon
            )(pt.trans)
          }
        ),
        div(cls := s"page-menu__content box $klass")(body)
      )
