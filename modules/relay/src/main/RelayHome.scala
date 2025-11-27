package lila.relay

import scalalib.paginator.Paginator

import lila.relay.RelayTour.WithLastRound

final class RelayHome(listing: RelayListing, pager: RelayPager, jsonView: RelayJsonView)(using Executor)(using
    scheduler: Scheduler
):

  def top(page: Int): Fu[(List[RelayCard], Paginator[WithLastRound])] =
    (page == 1).so(listing.active).zip(pager.inactive(page))

  def topJson(page: Int)(using RelayJsonView.Config) = top(page).map(jsonView.top)

  object spotlight:
    def get: List[RelayCard] = cache
    private var cache: List[RelayCard] = Nil
    scheduler.scheduleWithFixedDelay(3.seconds, 3.seconds): () =>
      listing.active.value match
        case Some(scala.util.Success(cards)) =>
          cache = cards
            .filter(_.tour.spotlight.exists(_.enabled))
            .filter: tr =>
              tr.display.hasStarted || tr.display.startsAtTime.exists(_.isBefore(nowInstant.plusMinutes(30)))
        case _ =>
