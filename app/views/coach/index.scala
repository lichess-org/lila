package views.html
package coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object index {

  def apply(pager: Paginator[lila.coach.Coach.WithUser], order: lila.coach.CoachPager.Order)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Lichess coaches",
      moreCss = cssTag("coach"),
      moreJs = infiniteScrollTag
    ) {
        main(cls := "coach-list coach-full-page")(
          st.aside(cls := "coach-list__side coach-side")(
            p(
              "Are you a great chess coach?", br,
              "Do you have a ", a(href := "https://lichess.org/help/master")("FIDE title"), "?", br,
              "Send us an email at ", contactEmailLink, br,
              "and we will review your application."
            )
          ),
          div(cls := "coach-list__main coach-main box")(
            div(cls := "box__top")(
              h1("Top chess coaches"),
              div(cls := "box__top__actions")(
                views.html.base.bits.mselect(
                  "coach-sort",
                  order.name,
                  lila.coach.CoachPager.Order.all map { o =>
                    a(href := routes.Coach.all(o.key), cls := (order == o).option("current"))(o.name)
                  }
                )
              )
            ),
            div(cls := "list infinitescroll")(
              pager.currentPageResults.map { c =>
                st.article(cls := "coach-widget paginated", attr("data-dedup") := c.coach.id.value)(widget(c, link = true))
              },
              pagerNext(pager, np => addQueryParameter(routes.Coach.all(order.key).url, "page", np)).map {
                frag(_, div(cls := "none")) // don't break the even/odd CSS flow
              }
            )
          )
        )
      }
}
