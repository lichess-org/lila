package views.html
package study

import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.study.Order

import controllers.routes

object search {

  def apply(pag: Paginator[lila.study.Study.WithChaptersAndLiked], text: String)(implicit ctx: Context) =
    views.html.base.layout(
      title = text,
      menu = Some(frag(
        a(href := routes.Study.all(Order.default.key))("All studies"),
        ctx.me.map { bits.authLinks(_, "search", Order.default) },
        a(cls := "text", dataIcon := "", href := "//lichess.org/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way")("What are studies?")
      )),
      moreCss = cssTag("studyList.css"),
      moreJs = infiniteScrollTag
    ) {
        div(cls := "content_box no_padding studies search")(
          div(cls := "top")(
            form(cls := "search", action := routes.Study.search(), method := "get")(
              input(name := "q", placeholder := trans.search.txt(), value := text)
            ),
            bits.newForm()
          ),
          list.paginate(pag, routes.Study.search(text))
        )
      }
}
