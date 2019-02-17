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
      moreCss = responsiveCssTag("coach"),
      responsive = true,
      moreJs = infiniteScrollTag
    ) {
        main(cls := "coach-list")(
          st.aside(cls := "coach-list__side")(
            img(src := staticUrl("images/icons/certification.svg"), cls := "coach-list__certification"),
            h2("Certified coaches"),
            p("We have curated this list of the best online chess coaches."),
            p("All of these renowned players have great chess skills, and a solid experience with teaching."),
            p("You cannot go wrong with them, so make your choice and enjoy learning chess!"),
            hr,
            p(
              "Are you a great chess coach?", br,
              "Do you have a ", a(href := "https://lichess.org/help/master")("FIDE title"), "?", br,
              "Send us an email at ", contactEmailLink, br,
              "and we will review your application."
            )
          ),
          div(cls := "coach-list__main box")(
            div(cls := "box__top")(
              h1("Top chess coaches"),
              div(cls := "box__top__actions")(
                views.html.base.bits.mselect(
                  "coach-sort",
                  order.name,
                  lila.coach.CoachPager.Order.all map { o =>
                    a(href := routes.Coach.all(o.key))(o.name)
                  }
                )
              )
            ),
            div(cls := "list infinitescroll")(
              pager.currentPageResults.map { c =>
                div(cls := "coach paginated", attr("data-dedup") := c.coach.id.value)(st.article(widget(c)))
              },
              pager.nextPage.map { np =>
                div(cls := "pager none")(
                  a(rel := "next", href := addQueryParameter(routes.Coach.all(order.key).toString, "page", np))("Next")
                )
              }
            )
          )
        )
      }
}
