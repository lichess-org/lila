package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

import controllers.routes

object bits {

  def layout(title: String, openGraph: Option[lila.app.ui.OpenGraph] = None, moreJs: Frag = emptyFrag)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = responsiveCssTag("blog"),
      moreJs = frag(prismicJs, moreJs),
      responsive = true,
      openGraph = openGraph,
      csp = defaultCsp.withPrismic(isGranted(_.Prismic)).some
    ) {
        main(cls := "blog page-small box box-pad")(body)
      }

  def metas(doc: io.prismic.Document)(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) =
    div(cls := "meta")(
      doc.getDate("blog.date").map { date =>
        span(cls := "text", dataIcon := "p")(semanticDate(date.value.toDateTimeAtStartOfDay))
      },
      doc.getText("blog.author").map { author =>
        span(cls := "text", dataIcon := "r")(richText(author))
      },
      doc.getText("blog.category").map { categ =>
        span(cls := "text", dataIcon := "t")(categ)
      }
    )
}
