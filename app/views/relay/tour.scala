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
      main(cls := "relay-index page-menu")(
        pageMenu("index"),
        div(cls := "page-menu__content box")(
          h1(liveBroadcasts()),
          st.section(
            active.map { tr =>
              div(cls := s"relay-widget relay-widget--active ${tierClass(tr.tour)}", dataIcon := "")(
                a(cls := "overlay", href                                                      := tr.path),
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
              div(cls := s"relay-widget ${tierClass(rt.tour)} paginated", dataIcon := "")(
                a(cls := "overlay", href                                           := rt.path),
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
      )
    }

  def page(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver, active: String)(implicit
      ctx: Context
  ) = {
    val title = ~doc.getText("doc.title")
    views.html.base.layout(
      title = title,
      moreCss = cssTag("page")
    ) {
      main(cls := "page-small page-menu")(
        pageMenu(active),
        div(cls := "page-menu__content box box-pad page")(
          h1(title),
          div(cls := "body")(raw(~doc.getHtml("doc.content", resolver)))
        )
      )
    }
  }

  private def layout(title: String, active: String)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relay.index")
    )(
      main(cls := "page-small page-menu")(
        pageMenu(active),
        body
      )
    )

  def pageMenu(menu: String)(implicit ctx: Context) =
    st.nav(cls := "page-menu__menu subnav")(
      a(href := routes.RelayTour.index(), cls := menu.activeO("index"))(trans.broadcast.broadcasts()),
      a(href := routes.RelayTour.calendar, cls := menu.activeO("calendar"))(trans.tournamentCalendar()),
      a(href := routes.RelayTour.form, cls := menu.activeO("new"))(trans.broadcast.newBroadcast()),
      a(href := routes.RelayTour.help, cls := menu.activeO("help"))("About broadcasts")
    )

  private def tierClass(tour: RelayTour) = s"tour-tier--${tour.tier | RelayTour.Tier.NORMAL}"
}
