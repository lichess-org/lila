package lila.relay
package ui

import scalalib.paginator.Paginator

import lila.core.LightUser
import lila.relay.RelayTour.WithLastRound
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class RelayTourUi(helpers: Helpers, ui: RelayUi):
  import helpers.{ *, given }
  import trans.{ broadcast as trc }

  def asRelayPager(p: Paginator[WithLastRound]): Paginator[RelayTour | WithLastRound] = p.mapResults(identity)

  def index(
      active: List[RelayTour.ActiveWithSomeRounds],
      upcoming: List[WithLastRound],
      past: Paginator[WithLastRound]
  )(using Context) =
    def nonEmptyTier(selector: RelayTour.Tier.Selector, tier: String) =
      val selected = active.filter(_.tour.tierIs(selector))
      selected.nonEmpty.option(st.section(cls := s"relay-cards relay-cards--tier-$tier"):
        selected.map:
          card.render(_, live = _.display.hasStarted)
      )
    Page(trc.liveBroadcasts.txt())
      .css("bits.relay.index")
      .js(infiniteScrollEsmInit)
      .hrefLangs(lila.ui.LangPath(routes.RelayTour.index())):
        main(cls := "relay-index page-menu")(
          pageMenu("index"),
          div(cls := "page-menu__content box box-pad")(
            boxTop(h1(trc.liveBroadcasts()), searchForm("")),
            Granter.opt(_.StudyAdmin).option(adminIndex(active)),
            nonEmptyTier(_.BEST, "best"),
            nonEmptyTier(_.HIGH, "high"),
            nonEmptyTier(_.NORMAL, "normal"),
            upcoming.nonEmpty.option(
              frag(
                h2(cls := "relay-index__section")("Upcoming broadcasts"),
                st.section(cls := "relay-cards relay-cards--upcoming"):
                  upcoming.map:
                    card.render(_, live = _ => false)
              )
            ),
            h2(cls := "relay-index__section")("Past broadcasts"),
            renderPager(asRelayPager(past), "")(cls := "relay-cards--past")
          )
        )

  private def adminIndex(active: List[RelayTour.ActiveWithSomeRounds])(using Context) =
    val errored = active.flatMap(a => a.errors.some.filter(_.nonEmpty).map(a -> _))
    errored.nonEmpty.option:
      div(cls := "relay-index__admin")(
        h2("Ongoing broadcasts with errors"),
        st.section(cls := "relay-cards"):
          errored.map: (tr, errors) =>
            card.render(tr.copy(link = tr.display), live = _.display.hasStarted, errors = errors.take(5))
      )

  private def listLayout(title: String, menu: Tag)(body: Modifier*)(using Context) =
    Page(trc.liveBroadcasts.txt())
      .css("bits.relay.index")
      .js(infiniteScrollEsmInit):
        main(cls := "relay-index page-menu")(menu, div(cls := "page-menu__content box box-pad")(body))

  def search(pager: Paginator[WithLastRound], query: String)(using Context) =
    listLayout(trc.liveBroadcasts.txt(), pageMenu("index"))(
      boxTop(
        h1(trc.liveBroadcasts()),
        searchForm(query)
      ),
      renderPager(asRelayPager(pager), query)(cls := "relay-cards--search")
    )

  def byOwner(pager: Paginator[RelayTour | WithLastRound], owner: LightUser)(using ctx: Context) =
    listLayout(trc.liveBroadcasts.txt(), pageMenu("by", owner.some))(
      boxTop(
        h1(
          if ctx.is(owner)
          then trc.myBroadcasts()
          else frag(lightUserLink(owner), " ", trc.liveBroadcasts())
        ),
        div(cls := "box__top__actions")(
          a(href := routes.RelayTour.form, cls := "button button-green text", dataIcon := Icon.PlusButton)(
            trc.newBroadcast()
          )
        )
      ),
      standardFlash,
      renderPager(pager, owner = owner.some)
    )

  def subscribed(pager: Paginator[RelayTour | WithLastRound])(using Context) =
    listLayout(trc.subscribedBroadcasts.txt(), pageMenu("subscribed"))(
      boxTop(h1(trc.subscribedBroadcasts())),
      standardFlash,
      renderPager(pager)(routes.RelayTour.subscribed)
    )

  def allPrivate(pager: Paginator[RelayTour | WithLastRound])(using Context) =
    listLayout("Private Broadcasts", pageMenu("allPrivate"))(
      boxTop(h1("Private Broadcasts")),
      renderPager(pager)(routes.RelayTour.allPrivate)
    )

  def showEmpty(t: RelayTour, owner: Option[LightUser], markup: Option[Html])(using Context) =
    Page(t.name.value).css("bits.page"):
      main(cls := "relay-tour page-menu")(
        pageMenu("by", owner),
        div(cls := "page-menu__content box box-pad page")(
          boxTop:
            ui.broadcastH1(t.name)
          ,
          h2(t.info.toString),
          markup.map: html =>
            frag(
              hr,
              div(cls := "body"):
                raw(html.value)
            )
        )
      )

  def page(title: String, pageBody: Frag, active: String)(using Context): Page =
    Page(title)
      .css("bits.page")
      .wrap: body =>
        main(cls := "page-small page-menu")(
          pageMenu(active),
          div(cls := "page-menu__content box box-pad page")(
            boxTop(ui.broadcastH1(title)),
            div(cls := "body")(pageBody)
          )
        )

  def pageMenu(menu: String, by: Option[LightUser] = none)(using ctx: Context): Tag =
    lila.ui.bits.pageMenuSubnav(
      a(href := routes.RelayTour.index(), cls := menu.activeO("index"))(trans.broadcast.broadcasts()),
      ctx.me.map: me =>
        a(
          href := routes.RelayTour.by(me.username, 1),
          cls  := (menu == "new" || by.exists(_.is(me))).option("active")
        ):
          trans.broadcast.myBroadcasts()
      ,
      by.filterNot(ctx.is)
        .map: user =>
          a(href := routes.RelayTour.by(user.name, 1), cls := "active")(
            user.name,
            " ",
            trans.broadcast.broadcasts()
          ),
      a(href := routes.RelayTour.subscribed(), cls := menu.activeO("subscribed"))(
        trans.broadcast.subscribedBroadcasts()
      ),
      Granter
        .opt(_.StudyAdmin)
        .option(
          a(href := routes.RelayTour.allPrivate(), cls := menu.activeO("allPrivate"))(
            "Private Broadcasts"
          )
        ),
      a(href := routes.RelayTour.calendar, cls := menu.activeO("calendar"))(trans.site.tournamentCalendar()),
      a(href := routes.RelayTour.help, cls := menu.activeO("help"))(trans.broadcast.aboutBroadcasts()),
      div(cls := "sep"),
      a(cls := menu.active("players"), href := routes.Fide.index(1))(trans.broadcast.fidePlayers()),
      a(cls := menu.active("federations"), href := routes.Fide.federations(1))(
        trans.broadcast.fideFederations()
      )
    )

  private object card:
    private def link(t: RelayTour, url: String, live: Boolean) = a(
      href := url,
      cls := List(
        "relay-card"         -> true,
        "relay-card--active" -> t.active,
        "relay-card--live"   -> live
      )
    )
    private def image(t: RelayTour) = t.image.fold(ui.thumbnail.fallback(cls := "relay-card__image")): id =>
      img(cls := "relay-card__image", src := ui.thumbnail.url(id, _.Size.Small))

    def render[A <: RelayRound.AndTourAndGroup](tr: A, live: A => Boolean, errors: List[String] = Nil)(using
        Context
    ) =
      link(tr.tour, tr.path, live(tr))(
        image(tr.tour),
        span(cls := "relay-card__body")(
          span(cls := "relay-card__info")(
            tr.tour.active.option(span(cls := "relay-card__round")(tr.display.name)),
            if live(tr)
            then
              span(cls := "relay-card__live")(
                "LIVE",
                tr.crowd
                  .filter(_ > 2)
                  .map: nb =>
                    span(cls := "relay-card__crowd text", dataIcon := Icon.User)(nb.localize)
              )
            else tr.display.startedAt.orElse(tr.display.startsAtTime).map(momentFromNow(_))
          ),
          h3(cls := "relay-card__title")(tr.group.fold(tr.tour.name.value)(_.value)),
          if errors.nonEmpty
          then ul(cls := "relay-card__errors")(errors.map(li(_)))
          else tr.tour.info.players.map(span(cls := "relay-card__desc")(_))
        )
      )

    def empty(t: RelayTour)(using Context) =
      link(t, routes.RelayTour.show(t.slug, t.id).url, false)(
        image(t),
        span(cls := "relay-card__body")(
          h3(cls := "relay-card__title")(t.name),
          span(cls := "relay-card__desc")(t.info.toString)
        )
      )

  private def searchForm(search: String)(using Context) = div(cls := "box__top__actions"):
    st.form(cls := "search", action := routes.RelayTour.index()):
      input(st.name := "q", value := search, placeholder := trans.search.search.txt())

  def renderPager(
      pager: Paginator[RelayTour | WithLastRound],
      query: String = "",
      owner: Option[LightUser] = None
  )(using Context): Tag = renderPager(pager): page =>
    owner match
      case None    => routes.RelayTour.index(page, query)
      case Some(u) => routes.RelayTour.by(u.name, page)

  def renderPager(pager: Paginator[RelayTour | WithLastRound])(next: Int => Call)(using Context): Tag =
    st.section(cls := "infinite-scroll relay-cards")(
      pager.currentPageResults.map:
        case w: WithLastRound => card.render(w, live = _ => false)(cls := "paginated")
        case t: RelayTour     => card.empty(t)(cls := "paginated")
      ,
      pagerNext(pager, next(_).url)
    )
