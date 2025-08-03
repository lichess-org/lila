package lila.relay
package ui

import java.time.YearMonth
import scalalib.paginator.Paginator

import lila.core.LightUser
import lila.relay.RelayTour.{ WithLastRound, WithFirstRound }
import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class RelayTourUi(helpers: Helpers, ui: RelayUi, card: RelayCardUi, pageMenu: RelayMenuUi):
  import helpers.{ *, given }
  import trans.broadcast as trc

  def asRelayPager(p: Paginator[WithLastRound]): Paginator[RelayTour | WithLastRound] = p.mapResults(identity)

  def index(
      active: List[RelayCard],
      past: Seq[WithLastRound]
  )(using Context) =
    def nonEmptyTier(selector: RelayTour.Tier.Selector) =
      val tier = RelayTour.Tier(selector)
      val selected = active.filter(_.tour.tierIs(selector))
      selected.nonEmpty.option(st.section(cls := s"relay-cards relay-cards--tier-$tier"):
        selected.map: sel =>
          card.render(sel, live = _.display.hasStarted, sel.crowd, alt = sel.alts.headOption))
    Page(trc.liveBroadcasts.txt())
      .css("bits.relay.index")
      .hrefLangs(lila.ui.LangPath(routes.RelayTour.index())):
        main(cls := "relay-index page-menu")(
          pageMenu("index"),
          div(cls := "page-menu__content box box-pad")(
            boxTop(h1(trc.liveBroadcasts()), searchForm("")),
            Granter.opt(_.StudyAdmin).option(adminIndex(active)),
            nonEmptyTier(_.best),
            nonEmptyTier(_.high),
            nonEmptyTier(_.normal),
            h2(cls := "relay-index__section")(trc.pastBroadcasts()),
            div(cls := "relay-cards"):
              past.map: t =>
                card.render(t, live = _ => false, crowd = Crowd(0))
            ,
            h2(cls := "relay-index__section relay-index__calendar"):
              a(cls := "button button-fat button-no-upper", href := routes.RelayTour.calendar)(
                strong(trc.broadcastCalendar()),
                small(trc.allBroadcastsByMonth())
              )
          )
        )

  private def adminIndex(active: List[RelayCard])(using Context) =
    val errored = for
      main <- active
      card <- main :: main.alts.map: alt =>
        RelayCard(
          tour = alt.tour,
          display = alt.round,
          link = alt.round,
          crowd = main.crowd,
          group = main.group,
          alts = Nil
        )
      errors <- card.errors.some.filter(_.nonEmpty)
    yield (card, errors)
    errored.nonEmpty.option:
      div(cls := "relay-index__admin")(
        h2("Ongoing broadcasts with errors"),
        st.section(cls := "relay-cards"):
          errored.map: (tr, errors) =>
            card.render(
              tr.copy(link = tr.display),
              live = _.display.hasStarted,
              crowd = tr.crowd,
              errors = errors.take(5)
            )
      )

  private def listLayout(title: String, menu: Tag)(body: Modifier*) =
    Page(title)
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

  def calendar(at: YearMonth, tours: List[WithFirstRound])(using ctx: Context) =
    Page(s"${trc.broadcastCalendar.txt()} ${showYearMonth(at)}")
      .css("bits.relay.calendar"):
        def dateForm(id: String) =
          lila.ui.bits.calendarMselect(
            helpers,
            id,
            allYears = RelayCalendar.allYears,
            firstMonth = monthOfFirstRelay,
            url = routes.RelayTour.calendarMonth
          )(at)
        main(cls := "relay-calendar page-menu")(
          pageMenu("calendar"),
          div(cls := "page-menu__content box box-pad")(
            boxTop(h1(dataIcon := Icon.RadioTower, cls := "text")(trc.broadcastCalendar()), searchForm("")),
            dateForm("top"),
            div(cls := "relay-cards"):
              tours.map(card.renderCalendar)
            ,
            (tours.sizeIs > 8).option(dateForm("bottom"))
          )
        )

  def showEmpty(t: RelayTour, owner: Option[LightUser], markup: Option[Html])(using Context) =
    Page(t.name.value).css("bits.page"):
      main(cls := "relay-tour page-menu")(
        pageMenu("by", owner),
        div(cls := "page-menu__content box box-pad page")(
          boxTop(ui.broadcastH1(t.name)),
          h2(t.info.toString),
          markup.map: html =>
            frag(
              hr,
              div(cls := "body"):
                raw(html.value)
            )
        )
      )

  def page(title: String, active: String)(using Context): Page =
    Page(title)
      .css("bits.page")
      .js(Esm("bits.expandText"))
      .wrap: body =>
        main(cls := "page-small page-menu")(
          pageMenu(active),
          div(cls := "page-menu__content box box-pad page")(
            boxTop(ui.broadcastH1(title)),
            div(cls := "body expand-text")(body)
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
      case None => routes.RelayTour.index(page, query)
      case Some(u) => routes.RelayTour.by(u.name, page)

  def renderPager(pager: Paginator[RelayTour | WithLastRound])(next: Int => Call)(using Context): Tag =
    st.section(cls := "infinite-scroll relay-cards")(
      pager.currentPageResults.map:
        case w: WithLastRound => card.render(w, live = _ => false, crowd = Crowd(0))(cls := "paginated")
        case t: RelayTour => card.empty(t)(cls := "paginated")
      ,
      pagerNext(pager, next(_).url)
    )
