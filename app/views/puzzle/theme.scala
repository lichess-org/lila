package views
package html.puzzle

import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.api.Context

object theme {

  def list(implicit ctx: Context) =
    views.html.base.layout(
      title = "Puzzle themes",
      moreCss = cssTag("puzzle."),
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
}
