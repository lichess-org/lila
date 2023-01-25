package lila.swiss

import reactivemongo.api.ReadPreference

import lila.common.config
import lila.common.paginator.Paginator
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter

final class SwissRoundPager(mongo: SwissMongo)(using Executor):

  import BsonHandlers.given

  private val maxPerPage = config.MaxPerPage(50)

  def apply(swiss: Swiss, round: SwissRoundNumber, page: Int): Fu[Paginator[SwissPairing]] =
    Paginator(
      adapter = new Adapter[SwissPairing](
        collection = mongo.pairing,
        selector = SwissPairing.fields { f =>
          $doc(f.swissId -> swiss.id, f.round -> round)
        },
        projection = none,
        sort = $empty,
        readPreference = ReadPreference.secondaryPreferred
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )
