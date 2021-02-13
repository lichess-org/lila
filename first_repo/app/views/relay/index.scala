package views.html.relay

import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object index {

  import trans.broadcast._

  def apply(
      fresh: Option[lila.relay.Relay.Fresh],
      pager: Paginator[lila.relay.Relay.WithStudyAndLiked],
      url: Call
  )(implicit ctx: Context) = {

    def sublist(name: Frag, relays: Seq[lila.relay.Relay.WithStudyAndLiked]) =
      relays.nonEmpty option st.section(
        h2(name),
        div(cls := "list")(
          relays.map(show.widget(_))
        )
      )

    views.html.base.layout(
      title = liveBroadcasts.txt(),
      moreCss = cssTag("relay.index"),
      moreJs = infiniteScrollTag
    ) {
      main(cls := "relay-index page-small box")(
        div(cls := "box__top")(
          h1(liveBroadcasts()),
          a(
            href := routes.Relay.form,
            cls := "new button button-empty",
            title := newBroadcast.txt(),
            dataIcon := "O"
          )
        ),
        fresh.map { f =>
          frag(
            sublist(ongoing(), f.started),
            sublist(upcoming(), f.created)
          )
        },
        st.section(
          h2(completed()),
          div(cls := "infinite-scroll")(
            pager.currentPageResults.map { show.widget(_, "paginated") },
            pagerNext(pager, np => addQueryParameter(url.url, "page", np))
          )
        )
      )
    }
  }
}
