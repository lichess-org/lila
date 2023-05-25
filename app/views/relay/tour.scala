package views.html.relay

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

import controllers.routes
import lila.relay.{ RelayRound, RelayTour }
import lila.relay.RelayTour.WithLastRound

object tour:

  import trans.broadcast.*

  def index(
      active: List[RelayTour.ActiveWithSomeRounds],
      pager: Paginator[RelayTour.WithLastRound],
      query: String = ""
  )(using Context) =
    views.html.base.layout(
      title = liveBroadcasts.txt(),
      moreCss = cssTag("relay.index"),
      moreJs = infiniteScrollTag
    ) {
      main(cls := "relay-index page-menu")(
        pageMenu("index"),
        div(cls := "page-menu__content box")(
          boxTop(
            h1(liveBroadcasts()),
            searchForm(query)
          ),
          st.section(
            active.map { renderWidget(_, ongoing = _.ongoing) }
          ),
          renderPager(pager, query)
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

  private def renderWidget[A <: RelayRound.AndTour](tr: A, ongoing: A => Boolean)(using Context) =
    div(
      cls := List(
        "relay-widget"                                        -> true,
        s"tour-tier--${tr.tour.tier | RelayTour.Tier.NORMAL}" -> true,
        "relay-widget--active"                                -> tr.tour.active,
        "relay-widget--ongoing"                               -> ongoing(tr)
      ),
      dataIcon := licon.RadioTower
    )(
      a(cls := "overlay", href := tr.path),
      div(
        h2(tr.tour.name),
        div(cls := "relay-widget__info")(
          p(tr.tour.description),
          p(cls := "relay-widget__info__meta")(
            tr.tour.active option frag(strong(tr.display.name), br),
            if ongoing(tr) then trans.playingRightNow() else tr.display.startsAt.map(momentFromNow(_))
          )
        )
      )
    )

  private def searchForm(search: String)(using Context) = div(cls := "box__top__actions")(
    st.form(cls := "search", action := routes.RelayTour.index())(
      input(st.name := "q", value := search, placeholder := trans.search.search.txt())
    )
  )

  private def renderPager(pager: Paginator[WithLastRound], query: String)(using Context) =
    st.section(cls := "infinite-scroll")(
      pager.currentPageResults map { tr =>
        renderWidget(tr, ongoing = _ => false)(cls := "paginated")
      },
      pagerNext(pager, routes.RelayTour.index(_, query).url)
    )
