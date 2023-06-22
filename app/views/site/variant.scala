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
      shogiVariant: shogi.variant.Variant
  )(implicit ctx: Context) =
    layout(
      activeKey = shogiVariant.key.some,
      title = variantName(shogiVariant),
      klass = "box-pad page variant",
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${variantName(shogiVariant)} - ${trans.variant.txt()}",
          url = s"$netBaseUrl${routes.Page.variant(shogiVariant.key)}",
          description = variantDescription(shogiVariant)
        )
        .some,
      withHrefLangs = doc.getText("variant.translated").isDefined option lila.i18n.LangList.EnglishJapanese
    )(
      h1(cls := "text", dataIcon := variantIcon(shogiVariant))(variantName(shogiVariant)),
      h2(cls := "headline")(variantDescription(shogiVariant)),
      div(cls := "body")(raw(~doc.getHtml("variant.content", resolver)))
    )

  def home(implicit ctx: Context) =
    layout(
      title = s"Lishogi ${trans.variants.txt()}",
      klass = "variants"
    )(
      h1(s"Lishogi ${trans.variants.txt()}"),
      div(cls := "variants")(
        shogi.variant.Variant.all.withFilter(!_.standard) map { v =>
          a(
            cls      := "variant text box__pad",
            href     := routes.Page.variant(v.key),
            dataIcon := variantIcon(v)
          )(
            span(
              h2(variantName(v)),
              h3(cls := "headline")(variantDescription(v))
            )
          )
        }
      )
    )

  private def layout(
      title: String,
      klass: String,
      activeKey: Option[String] = None,
      openGraph: Option[lila.app.ui.OpenGraph] = None,
      withHrefLangs: Option[lila.i18n.LangList.AlternativeLangs] = None
  )(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("variant"),
      openGraph = openGraph,
      withHrefLangs = withHrefLangs
    )(
      main(cls := "page-menu")(
        st.aside(cls := "page-menu__menu subnav")(
          shogi.variant.Variant.all.withFilter(!_.standard) map { v =>
            a(
              cls      := List("text" -> true, "active" -> activeKey.has(v.key)),
              href     := routes.Page.variant(v.key),
              dataIcon := variantIcon(v)
            )(variantName(v))
          }
        ),
        div(cls := s"page-menu__content box $klass")(body)
      )
    )
}
