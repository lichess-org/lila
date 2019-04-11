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
  )(implicit ctx: Context) = layout(
    active = perfType.some,
    title = s"${variant.name} • ${variant.title}",
    klass = "box-pad page variant"
  )(
    h1(cls := "text", dataIcon := perfType.iconChar)(variant.name),
    h2(cls := "headline")(variant.title),
    div(cls := "body")(raw(~doc.getHtml("variant.content", resolver)))
  )

  def home()(implicit ctx: Context) = layout(
    title = trans.rulesAndVariants.txt(),
    klass = "variants"
  )(
      h1(trans.rulesAndVariants()),
      div(cls := "body box__pad")(
        p(trans.standardFmjdRegulationsWithDrawingRules()),
        p(trans.exploreDraughtsVariants())
      ),
      div(cls := "variants")(
        lidraughts.rating.PerfType.variantsPlus map { pt =>
          val variant = lidraughts.rating.PerfType variantOf pt
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
    active: Option[lidraughts.rating.PerfType] = None,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None
  )(body: Modifier*)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = responsiveCssTag("variant"),
    openGraph = openGraph
  )(
      main(cls := "page-menu")(
        st.aside(cls := "page-menu__menu subnav")(
          lidraughts.rating.PerfType.variantsPlus map { pt =>
            a(
              cls := List("text" -> true, "active" -> active.has(pt)),
              href := routes.Page.variant(pt.key),
              dataIcon := pt.iconChar
            )(pt.name)
          }
        ),
        div(cls := s"page-menu__content box $klass")(body)
      )
    )
}
