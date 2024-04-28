package views.relay

import lila.app.templating.Environment.{ *, given }

import lila.core.LightUser
import scalalib.paginator.Paginator
import lila.relay.RelayTour.WithLastRound
import lila.relay.{ RelayRound, RelayTour }
import scalatags.Text.TypedTag
import scalatags.text.Builder

lazy val bits = lila.relay.ui.RelayBits(helpers)(views.study.jsI18n)

object tour:

  import trans.broadcast.*

  def asRelayPager(p: Paginator[WithLastRound]): Paginator[RelayTour | WithLastRound] = p.mapResults(identity)

  def index(
      active: List[RelayTour.ActiveWithSomeRounds],
      upcoming: List[WithLastRound],
      past: Paginator[WithLastRound]
  )(using PageContext) =
    views.base.layout(
      title = liveBroadcasts.txt(),
      moreCss = cssTag("relay.index"),
      modules = infiniteScrollEsmInit,
      withHrefLangs = lila.ui.LangPath(routes.RelayTour.index()).some
    ):
      def nonEmptyTier(selector: RelayTour.Tier.Selector, tier: String) =
        val selected = active.filter(_.tour.tierIs(selector))
        selected.nonEmpty.option(st.section(cls := s"relay-cards relay-cards--tier-$tier"):
          selected.map:
            card.render(_, ongoing = _.ongoing)
        )

      main(cls := "relay-index page-menu")(
        pageMenu("index"),
        div(cls := "page-menu__content box box-pad")(
          boxTop(h1(liveBroadcasts()), searchForm("")),
          nonEmptyTier(_.BEST, "best"),
          nonEmptyTier(_.HIGH, "high"),
          nonEmptyTier(_.NORMAL, "normal"),
          upcoming.nonEmpty.option(
            frag(
              h2(cls := "relay-index__section")("Upcoming broadcasts"),
              st.section(cls := "relay-cards relay-cards--upcoming"):
                upcoming.map:
                  card.render(_, ongoing = _ => false)
            )
          ),
          h2(cls := "relay-index__section")("Past broadcasts"),
          renderPager(asRelayPager(past), "")(cls := "relay-cards--past")
        )
      )

  private def listLayout(title: String, menu: Tag)(body: Modifier*)(using PageContext) =
    views.base.layout(
      title = liveBroadcasts.txt(),
      moreCss = cssTag("relay.index"),
      modules = infiniteScrollEsmInit
    )(main(cls := "relay-index page-menu")(div(cls := "page-menu__content box box-pad")(body)))

  def search(pager: Paginator[WithLastRound], query: String)(using PageContext) =
    listLayout(liveBroadcasts.txt(), pageMenu("index"))(
      boxTop(
        h1(liveBroadcasts()),
        searchForm(query)
      ),
      renderPager(asRelayPager(pager), query)(cls := "relay-cards--search")
    )

  def byOwner(pager: Paginator[RelayTour | WithLastRound], owner: LightUser)(using PageContext) =
    listLayout(liveBroadcasts.txt(), pageMenu("by", owner.some))(
      boxTop(h1(lightUserLink(owner), " ", liveBroadcasts())),
      standardFlash,
      renderPager(pager, owner = owner.some)
    )

  def subscribed(pager: Paginator[RelayTour | WithLastRound])(using PageContext) =
    listLayout(subscribedBroadcasts.txt(), pageMenu("subscribed"))(
      boxTop(h1(subscribedBroadcasts())),
      standardFlash,
      renderPager(pager)
    )

  def allPrivate(pager: Paginator[RelayTour | WithLastRound])(using PageContext) =
    listLayout("Private Broadcasts", pageMenu("allPrivate"))(
      boxTop(h1("Private Broadcasts")),
      renderPager(pager)
    )

  def showEmpty(t: RelayTour, owner: Option[LightUser], markup: Option[Html])(using PageContext) =
    views.base.layout(
      title = t.name.value,
      moreCss = cssTag("page")
    ):
      main(cls := "relay-tour page-menu")(
        pageMenu("by", owner),
        div(cls := "page-menu__content box box-pad page")(
          boxTop:
            bits.broadcastH1(t.name)
          ,
          h2(t.description),
          markup.map: html =>
            frag(
              hr,
              div(cls := "body"):
                raw(html.value)
            )
        )
      )

  def page(p: lila.cms.CmsPage.Render, active: String)(using PageContext) =
    views.base.layout(
      title = p.title,
      moreCss = cssTag("page")
    ):
      main(cls := "page-small page-menu")(
        pageMenu(active),
        div(cls := "page-menu__content box box-pad page")(
          boxTop:
            bits.broadcastH1(p.title)
          ,
          div(cls := "body")(views.cms.render(p))
        )
      )

  def pageMenu(menu: String, by: Option[LightUser] = none)(using ctx: lila.ui.Context): Tag =
    lila.ui.bits.pageMenuSubnav(
      a(href := routes.RelayTour.index(), cls := menu.activeO("index"))(trans.broadcast.broadcasts()),
      ctx.me.map: me =>
        a(href := routes.RelayTour.by(me.username, 1), cls := by.exists(_.is(me)).option("active")):
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
      isGranted(_.StudyAdmin).option(
        a(href := routes.RelayTour.allPrivate(), cls := menu.activeO("allPrivate"))(
          "Private Broadcasts"
        )
      ),
      a(href := routes.RelayTour.form, cls := menu.activeO("new"))(trans.broadcast.newBroadcast()),
      a(href := routes.RelayTour.calendar, cls := menu.activeO("calendar"))(trans.site.tournamentCalendar()),
      a(href := routes.RelayTour.help, cls := menu.activeO("help"))(trans.broadcast.aboutBroadcasts()),
      div(cls := "sep"),
      a(cls := menu.active("players"), href := routes.Fide.index(1))("FIDE players"),
      a(cls := menu.active("federations"), href := routes.Fide.federations(1))("FIDE federations")
    )

  object thumbnail:
    def apply(image: Option[ImageId], size: RelayTour.thumbnail.SizeSelector): Tag =
      image.fold(fallback): id =>
        img(
          cls     := "relay-image",
          widthA  := size(RelayTour.thumbnail).width,
          heightA := size(RelayTour.thumbnail).height,
          src     := url(id, size)
        )
    def fallback = iconTag(Icon.RadioTower)(cls := "relay-image--fallback")
    def url(id: ImageId, size: RelayTour.thumbnail.SizeSelector) =
      RelayTour.thumbnail(picfitUrl, id, size)

  private object card:
    private def link(t: RelayTour, url: String, ongoing: Boolean) = a(
      href := url,
      cls := List(
        "relay-card"          -> true,
        "relay-card--active"  -> t.active,
        "relay-card--ongoing" -> ongoing
      )
    )
    private def image(t: RelayTour) = t.image.fold(thumbnail.fallback(cls := "relay-card__image")): id =>
      img(cls := "relay-card__image", src := thumbnail.url(id, _.Size.Small))

    def render[A <: RelayRound.AndTourAndGroup](tr: A, ongoing: A => Boolean)(using Context) =
      link(tr.tour, tr.path, ongoing(tr))(
        image(tr.tour),
        span(cls := "relay-card__body")(
          span(cls := "relay-card__info")(
            tr.tour.active.option(span(cls := "relay-card__round")(tr.display.name)),
            if ongoing(tr)
            then
              span(cls := "relay-card__live")(
                "LIVE",
                tr.crowd
                  .filter(_ > 2)
                  .map: nb =>
                    span(cls := "relay-card__crowd text", dataIcon := Icon.User)(nb.localize)
              )
            else tr.display.startedAt.orElse(tr.display.startsAt).map(momentFromNow(_))
          ),
          h3(cls := "relay-card__title")(tr.group.fold(tr.tour.name.value)(_.value)),
          span(cls := "relay-card__desc")(tr.tour.description)
        )
      )

    def empty(t: RelayTour)(using Context) =
      link(t, routes.RelayTour.show(t.slug, t.id).url, false)(
        image(t),
        span(cls := "relay-card__body")(
          h3(cls := "relay-card__title")(t.name),
          span(cls := "relay-card__desc")(t.description)
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
        case w: WithLastRound => card.render(w, ongoing = _ => false)(cls := "paginated")
        case t: RelayTour     => card.empty(t)(cls := "paginated")
      ,
      pagerNext(pager, next(_).url)
    )
