package lila.relay

import play.api.libs.json.JsObject
import scalalib.paginator.Paginator

import lila.core.i18n.Translate
import lila.core.net.Continent
import lila.relay.RelayTour.WithLastRound

case class RelayHome(ongoing: List[RelayCard], recent: List[WithLastRound], past: Paginator[WithLastRound])

final class RelayHomeApi(listing: RelayListing, pager: RelayPager, jsonView: RelayJsonView)(using Executor)(
    using scheduler: Scheduler
):

  def cacheTtl(page: Int) = if page == 1 then 10.seconds else 10.minutes

  def get: Fu[RelayHome] = for
    active <- listing.active
    past <- pager.inactive(1)
    (recent, reallyPast) = stealRecentFromPast(past.currentPageResults)
  yield RelayHome(active, recent, past.withCurrentPageResults(reallyPast))

  def getJson(page: Int)(using RelayJsonView.Config, Translate): Fu[JsObject] =
    if page == 1 then get.map(jsonView.home)
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
    def getForContinent(continent: Option[Continent]): List[RelayCard] =
      cache.filter(_.tour.spotlight.forall(_.visibleFrom(continent)))
    def hasContinentRestriction: Boolean = cache.exists(_.tour.spotlight.exists(_.continent.isDefined))
    private var cache: List[RelayCard] = Nil
    scheduler.scheduleWithFixedDelay(cacheTtl(1), listing.activeCacheTtl): () =>
      listing.active.value.foreach:
        _.foreach: cards =>
          cache = cards
            .filter(_.tour.spotlight.exists(_.enabled))
            .filter: tr =>
              tr.display.hasStarted || tr.display.startsAtTime.exists(_.isBefore(nowInstant.plusMinutes(30)))
