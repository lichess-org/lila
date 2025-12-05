package lila.relay

import scalalib.paginator.Paginator

import lila.relay.RelayTour.WithLastRound
import lila.core.i18n.Translate
import play.api.libs.json.JsObject

case class RelayHome(ongoing: List[RelayCard], recent: List[WithLastRound], past: Paginator[WithLastRound])

final class RelayHomeApi(listing: RelayListing, pager: RelayPager, jsonView: RelayJsonView)(using Executor)(
    using scheduler: Scheduler
):

  def home: Fu[RelayHome] = for
    active <- listing.active
    past <- pager.inactive(1)
    (recent, reallyPast) = stealRecentFromPast(past.currentPageResults)
  yield RelayHome(active, recent, past.withCurrentPageResults(reallyPast))

  def get(page: Int): Fu[RelayHome | Paginator[WithLastRound]] =
    if page == 1 then home
    else pager.inactive(page)

  def getJson(page: Int)(using RelayJsonView.Config, Translate): Fu[JsObject] =
    if page == 1 then home.map(jsonView.home)
    else pager.inactive(page).map(jsonView.top(Nil, _))

  private def stealRecentFromPast(past: Seq[WithLastRound]): (List[WithLastRound], List[WithLastRound]) =
    val base = nowInstant
    past.toList.partition: t =>
      val hoursBonus = t.tour.tier.so:
        case RelayTour.Tier.best => 24
        case RelayTour.Tier.high => 6
        case RelayTour.Tier.normal => 2
      t.display.finishedAt.orElse(t.display.startsAtTime).exists(_.isAfter(base.minusHours(hoursBonus)))

  object spotlight:
    def get: List[RelayCard] = cache
    private var cache: List[RelayCard] = Nil
    scheduler.scheduleWithFixedDelay(10.seconds, listing.activeCacheTtl): () =>
      listing.active.value.foreach:
        _.foreach: cards =>
          cache = cards
            .filter(_.tour.spotlight.exists(_.enabled))
            .filter: tr =>
              tr.display.hasStarted || tr.display.startsAtTime.exists(_.isBefore(nowInstant.plusMinutes(30)))
