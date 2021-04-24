package views.html.relay

import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes
import lila.relay.RelayRound

object index {

  import trans.broadcast._

  def apply(
      fresh: Option[RelayRound.Fresh],
      pager: Paginator[RelayRound.WithTour]
  )(implicit ctx: Context) = {

    def sublist(name: Frag, relays: Seq[RelayRound.WithTour]) =
      relays.nonEmpty option st.section(
        h2(name),
        div(cls := "list")(relays.map(widget))
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
            href := routes.RelayTour.form,
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
            pager.currentPageResults map widget,
            pagerNext(pager, routes.RelayTour.index(_).url)
          )
        )
      )
    }
  }

  private def widget(rt: RelayRound.WithTour)(implicit ctx: Context) =
    div(cls := "relay-widget paginaated", dataIcon := "")(
      a(cls := "overlay", href := rt.path),
      div(
        h3(rt.tour.name),
        p(strong(rt.relay.name), " • ", rt.relay.showStartAt.map(momentFromNow(_)))
      )
    )
}
