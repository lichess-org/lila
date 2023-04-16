package views.html.relay

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

import controllers.routes
import lila.relay.RelayTour

object tour:

  import trans.broadcast.*

  def index(
      active: List[RelayTour.ActiveWithNextRound],
      pager: Paginator[RelayTour.WithLastRound]
  )(using Context) =
    views.html.base.layout(
      title = liveBroadcasts.txt(),
      moreCss = cssTag("relay.index"),
      moreJs = infiniteScrollTag
    ) {
      main(cls := "relay-index page-menu")(
        pageMenu("index"),
        div(cls := "page-menu__content box")(
          boxTop(h1(liveBroadcasts())),
          st.section(
            active.map { tr =>
              div(
                cls := List(
                  "relay-widget relay-widget--active" -> true,
                  tierClass(tr.tour)                  -> true,
                  "relay-widget--ongoing"             -> tr.ongoing
                ),
                dataIcon := ""
              )(
                a(cls := "overlay", href := tr.path),
                div(
                  h2(tr.tour.name),
                  div(cls := "relay-widget__info")(
                    p(tr.tour.description),
                    p(cls := "relay-widget__info__meta")(
                      strong(tr.round.name),
                      br,
                      if tr.ongoing
                      then trans.playingRightNow()
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
      )
    }

  def page(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver, active: String)(using
      Context
  ) =
    val title = ~doc.getText("doc.title")
    views.html.base.layout(
      title = title,
      moreCss = cssTag("page")
    ) {
      main(cls := "page-small page-menu")(
        pageMenu(active),
        div(cls := "page-menu__content box box-pad page")(
          boxTop(title),
          div(cls := "body")(raw(~doc.getHtml("doc.content", resolver)))
        )
      )
    }

  def pageMenu(menu: String)(using Context) =
    st.nav(cls := "page-menu__menu subnav")(
      a(href := routes.RelayTour.index(), cls := menu.activeO("index"))(trans.broadcast.broadcasts()),
      a(href := routes.RelayTour.calendar, cls := menu.activeO("calendar"))(trans.tournamentCalendar()),
      a(href := routes.RelayTour.form, cls := menu.activeO("new"))(trans.broadcast.newBroadcast()),
      a(href := routes.RelayTour.help, cls := menu.activeO("help"))("About broadcasts")
    )

  private def tierClass(tour: RelayTour) = s"tour-tier--${tour.tier | RelayTour.Tier.NORMAL}"
