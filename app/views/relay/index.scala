package views.html.relay

import play.api.mvc.Call

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator

import controllers.routes

object index {

  def apply(
    fresh: Option[lidraughts.relay.Relay.Fresh],
    pager: Paginator[lidraughts.relay.Relay.WithStudyAndLiked],
    url: Call
  )(implicit ctx: Context) = {

    def sublist(name: String, relays: Seq[lidraughts.relay.Relay.WithStudyAndLiked]) =
      relays.nonEmpty option st.section(
        h2(name),
        div(cls := "list")(
          relays.map(show.widget(_))
        )
      )

    views.html.base.layout(
      title = "Live tournament broadcasts",
      moreCss = responsiveCssTag("relay.index"),
      moreJs = infiniteScrollTag
    ) {
        main(cls := "relay-index page-small box")(
          div(cls := "box__top")(
            h1("Live tournament broadcasts"),
            isGranted(_.Relay) option a(href := routes.Relay.form, cls := "new button button-empty", title := "New Broadcast", dataIcon := "O")
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
