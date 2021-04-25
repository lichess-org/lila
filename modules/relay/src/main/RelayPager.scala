package lila.relay

import reactivemongo.api.ReadPreference

import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }

final class RelayPager(tourRepo: RelayTourRepo)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import BSONHandlers._

  def inactive(page: Int): Fu[Paginator[RelayTour]] =
    Paginator(
      adapter = new CachedAdapter(
        nbResults = fuccess(9999),
        adapter = new Adapter[RelayTour](
          collection = tourRepo.coll,
          selector = tourRepo.selectors.official ++ tourRepo.selectors.inactive,
          projection = none,
          sort = $sort desc "syncedAt",
          readPreference = ReadPreference.secondaryPreferred
        )
      ),
      currentPage = page,
      maxPerPage = MaxPerPage(20)
    )
}
