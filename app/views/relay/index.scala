package views.html.relay

import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object index {

  def apply(
    fresh: Option[lila.relay.Relay.Fresh],
    pager: Paginator[lila.relay.Relay.WithStudyAndLiked],
    url: Call
  )(implicit ctx: Context) = {

    def sublist(name: String, relays: Seq[lila.relay.Relay.WithStudyAndLiked]) =
      relays.nonEmpty option st.section(
        h2(name),
        div(cls := "list")(
          relays.map(show.widget(_))
        )
      )

    views.html.base.layout(
      title = "Live tournament broadcasts",
      moreCss = cssTag("relay.index"),
      moreJs = infiniteScrollTag
    ) {
        main(cls := "relay-index page-small box")(
          div(cls := "box__top")(
            h1("Live tournament broadcasts"),
            a(href := routes.Relay.form, cls := "new button button-empty", title := "New Broadcast", dataIcon := "O")
          ),
          fresh.map { f =>
            frag(
              sublist("Ongoing", f.started),
              sublist("Upcoming", f.created)
            )
          },
          st.section(
            h2("Completed"),
            div(cls := "infinitescroll")(
              pager.currentPageResults.map { show.widget(_, "paginated") },
              pagerNext(pager, np => addQueryParameter(url.url, "page", np))
            )
          )
        )
      }
  }
}
