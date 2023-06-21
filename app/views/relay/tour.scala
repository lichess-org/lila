package views.html.relay

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

import controllers.routes
import lila.relay.{ RelayRound, RelayTour }
import lila.relay.RelayTour.WithLastRound
import lila.common.LightUser

object tour:

  import trans.broadcast.*

  def index(
      active: List[RelayTour.ActiveWithSomeRounds],
      pager: Paginator[RelayTour.WithLastRound],
      query: String = ""
  )(using PageContext) =
    views.html.base.layout(
      title = liveBroadcasts.txt(),
      moreCss = cssTag("relay.index"),
      moreJs = infiniteScrollTag
    ):
      main(cls := "relay-index page-menu")(
        pageMenu("index"),
        div(cls := "page-menu__content box")(
          boxTop(
            h1(liveBroadcasts()),
            searchForm(query)
          ),
          st.section:
            active.map { renderWidget(_, ongoing = _.ongoing) }
          ,
          renderPager(pager, query)
        )
      )

  def byOwner(pager: Paginator[RelayTour.WithLastRound], owner: LightUser)(using PageContext) =
    views.html.base.layout(
      title = liveBroadcasts.txt(),
      moreCss = cssTag("relay.index"),
      moreJs = infiniteScrollTag
    ):
      main(cls := "relay-index page-menu")(
        pageMenu("by", owner.some),
        div(cls := "page-menu__content box")(
          boxTop:
            h1(lightUserLink(owner), " ", liveBroadcasts())
          ,
          standardFlash,
          renderPager(pager, owner = owner.some)
        )
      )

  def page(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver, active: String)(using
      PageContext
  ) =
    val title = ~doc.getText("doc.title")
    views.html.base.layout(
      title = title,
      moreCss = cssTag("page")
    ):
      main(cls := "page-small page-menu")(
        pageMenu(active),
        div(cls := "page-menu__content box box-pad page")(
          boxTop(title),
          div(cls := "body")(raw(~doc.getHtml("doc.content", resolver)))
        )
      )

  def pageMenu(menu: String, by: Option[LightUser] = none)(using ctx: PageContext) =
    st.nav(cls := "page-menu__menu subnav")(
      a(href := routes.RelayTour.index(), cls := menu.activeO("index"))(trans.broadcast.broadcasts()),
      ctx.me.map: me =>
        a(href := routes.RelayTour.by(me.username, 1), cls := by.exists(_ is me).option("active"))(
          "My broadcasts"
        ),
      by.filterNot(ctx.is)
        .map: user =>
          a(href := routes.RelayTour.by(user.name, 1), cls := "active")(
            user.name,
            " ",
            trans.broadcast.broadcasts()
          ),
      a(href := routes.RelayTour.form, cls := menu.activeO("new"))(trans.broadcast.newBroadcast()),
      a(href := routes.RelayTour.calendar, cls := menu.activeO("calendar"))(trans.tournamentCalendar()),
      a(href := routes.RelayTour.help, cls := menu.activeO("help"))("About broadcasts")
    )

  private def renderWidget[A <: RelayRound.AndTour](tr: A, ongoing: A => Boolean)(using PageContext) =
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
            if ongoing(tr)
            then trans.playingRightNow()
            else tr.display.startedAt.orElse(tr.display.startsAt).map(momentFromNow(_))
          )
        )
      )
    )

  private def searchForm(search: String)(using PageContext) = div(cls := "box__top__actions")(
    st.form(cls := "search", action := routes.RelayTour.index())(
      input(st.name := "q", value := search, placeholder := trans.search.search.txt())
    )
  )

  private def renderPager(
      pager: Paginator[WithLastRound],
      query: String = "",
      owner: Option[LightUser] = None
  )(using PageContext) =
    def next(page: Int) = owner match
      case None    => routes.RelayTour.index(page, query)
      case Some(u) => routes.RelayTour.by(u.name, page)
    st.section(cls := "infinite-scroll")(
      pager.currentPageResults.map: tr =>
        renderWidget(tr, ongoing = _ => false)(cls := "paginated"),
      pagerNext(pager, next(_).url)
    )
