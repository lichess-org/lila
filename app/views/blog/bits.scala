package views.html.blog

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.richText

import controllers.routes

object bits {

  def layout(title: String, openGraph: Option[lidraughts.app.ui.OpenGraph] = None, moreJs: Frag = emptyFrag)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("blog"),
      moreJs = frag(prismicJs, moreJs),
      openGraph = openGraph,
      csp = defaultCsp.withPrismic(isGranted(_.Prismic)).some
    ) {
        main(cls := "blog page-small box box-pad")(body)
      }

  def metas(doc: io.prismic.Document)(implicit ctx: Context, prismic: lidraughts.blog.BlogApi.Context) =
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
