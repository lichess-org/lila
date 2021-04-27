package views.html.relay

import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes
import lila.relay.{ RelayRound, RelayTour }

object tour {

  import trans.broadcast._

  def index(
      active: List[RelayTour.ActiveWithNextRound],
      pager: Paginator[RelayTour.WithLastRound]
  )(implicit ctx: Context) =
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
        st.section(
          active.map { tr =>
            div(cls := "relay-widget relay-widget--active", dataIcon := "")(
              a(cls := "overlay", href := tr.path),
              div(
                h2(tr.tour.name),
                div(cls := "relay-widget__info")(
                  p(tr.tour.description),
                  p(cls := "relay-widget__info__meta")(
                    strong(tr.round.name),
                    br,
                    if (tr.ongoing) trans.playingRightNow()
                    else tr.round.startsAt.map(momentFromNow(_))
                  )
                )
              )
            )
          }
        ),
        st.section(cls := "infinite-scroll")(
          pager.currentPageResults map { rt =>
            div(cls := "relay-widget paginated", dataIcon := "")(
              a(cls := "overlay", href := rt.path),
              div(
                h2(rt.tour.name),
                div(cls := "relay-widget__info")(
                  p(rt.tour.description),
                  rt.tour.syncedAt.map(momentFromNow(_)(cls := "relay-widget__info__meta"))
                )
              )
            )
          },
          pagerNext(pager, routes.RelayTour.index(_).url)
        )
      )
    }

}
